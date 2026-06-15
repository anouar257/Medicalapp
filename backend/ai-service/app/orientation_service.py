import logging
from typing import Dict, Any, List
from app.gemini_client import query_gemini
from app.specialty_rules import get_fallback_orientation
from app.safety import sanitize_response

logger = logging.getLogger(__name__)

SHORT_ZONES = [
    # French
    "tête", "tete", "ventre", "estomac", "abdomen", "dos", "dents", "dent", "gorge", "poitrine", "coeur", "cœur", "yeux", "œil", "oeil", "peau", "genou", "genoux", "articulation", "oreille", "oreilles",
    # English
    "head", "migraine", "stomach", "belly", "abdomen", "back", "teeth", "tooth", "throat", "chest", "heart", "eye", "eyes", "skin", "knee", "knees", "joint", "ear", "ears",
    # Arabic
    "رأس", "راس", "الرأس", "الراس", "بطن", "البطن", "معدة", "المعدة", "معده", "المعده", "ظهر", "الظهر", "أسنان", "الأسنان", "اسنان", "الاسنان", "سن", "السن", "ضرس", "الضرس", "حلق", "الحلق", "حنجرة", "الحنجرة", "حنجره", "الحنجره", "صدر", "الصدر", "قلب", "القلب", "عين", "العين", "عيون", "العيون", "جلد", "الجلد", "بشرة", "البشرة", "بشره", "البشره", "ركبة", "الركبة", "ركبه", "الركبه", "مفاصل", "المفاصل", "أذن", "الأذن", "إذن", "الاذن", "اذن"
]

def reconstruct_context(message: str, language: str, history: List[Dict[str, str]]) -> str:
    if not history or len(history) < 2:
        return message
        
    last_assistant = history[-1]
    # Pydantic model might be passed as object or dict. Let's handle both.
    role = last_assistant.role if hasattr(last_assistant, "role") else last_assistant.get("role")
    content = last_assistant.content if hasattr(last_assistant, "content") else last_assistant.get("content")
    
    if role != "assistant":
        return message
        
    last_content = (content or "").lower()
    is_precision_request = any(kw in last_content for kw in ["préciser", "zone concernée", "specify", "affected area", "تحديد", "المنطقة المصابة", "symptôme", "symptoms", "أعراض"])
    
    msg_clean = message.strip().lower()
    is_short_zone = len(msg_clean) <= 15 and any(zone in msg_clean for zone in SHORT_ZONES)
    
    if is_precision_request and is_short_zone:
        # Find last user message
        last_user_msg = ""
        for msg in reversed(history[:-1]):
            m_role = msg.role if hasattr(msg, "role") else msg.get("role")
            m_content = msg.content if hasattr(msg, "content") else msg.get("content")
            if m_role == "user":
                last_user_msg = m_content or ""
                break
                
        if last_user_msg:
            from app.specialty_rules import reconstruct_message
            reconstructed = reconstruct_message(last_user_msg, message, language)
            logger.info(f"Reconstructed message context: '{last_user_msg}' + '{message}' ➜ '{reconstructed}'")
            return reconstructed
            
    return message

async def get_orientation(message: str, language: str, history: List[Any] = None, db_specialties: List[Any] = None) -> Dict[str, Any]:
    # Reconstruct message if history is provided
    final_message = reconstruct_context(message, language, history or [])
    
    # Try calling Gemini first
    try:
        data = await query_gemini(final_message, language, db_specialties or [])
        if data:
            return sanitize_response(data)
    except Exception as e:
        logger.error(f"Unexpected error in query_gemini calling: {type(e).__name__}")
        
    # Fallback to local rule engine
    logger.info("Falling back to local specialty rules engine.")
    fallback_data = get_fallback_orientation(final_message, language, db_specialties or [])
    return sanitize_response(fallback_data)
