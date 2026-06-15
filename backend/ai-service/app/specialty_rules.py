import re
from typing import List, Dict, Any

# Keyword rules mapping to key/specialty names
# We map keywords to their neutral IDs, then translate them to the requested language.
SPECIALTIES_KEYWORDS = {
    "dentist": ["dent", "dentiste", "gencive", "carie", "plombage", "tooth", "teeth", "dentist", "toothache", "سن", "أسنان", "تسوس", "لثة", "ضرس"],
    "ent": ["gorge", "oreille", "nez", "otite", "sinus", "throat", "ear", "nose", "tonsil", "angine", "rhume", "nez bouché", "حنجرة", "أذن", "أنف", "لوزتين", "التهاب الحلق"],
    "cardiologist": ["cœur", "coeur", "poitrine", "palpitation", "cardio", "tachycardie", "heart", "chest", "cardi", "قلب", "صدر", "خفقان"],
    "dermatologist": ["peau", "bouton", "allergie", "eczema", "skin", "rash", "dermat", "acné", "acne", "جلد", "حكة", "بثور", "حساسية الجلد", "أكزيما"],
    "rheumatologist": ["dos", "articulation", "genou", "muscle", "bone", "joint", "back", "orthop", "rheu", "rhumatisme", "arthrose", "mal de dos", "ظهر", "مفاصل", "ركبة", "عظام", "روماتيزم"],
    "pediatrician": ["enfant", "bebe", "bébé", "child", "baby", "pediatric", "nourrisson", "طفل", "أطفال", "رضيع"],
    "ophthalmologist": ["yeux", "oeil", "œil", "vision", "eye", "ophthalm", "lunettes", "myope", "عيون", "عين", "نظر", "نظارات"],
    "gynecologist": ["grossesse", "enceinte", "regles", "règles", "gynec", "pregnan", "gynéco", "accouche", "حامل", "حمل", "نساء", "توليد", "دورة شهرية"],
    "gp": ["fièvre", "fievre", "fever", "toux", "cough", "fatigue", "grippe", "flu", "courbature", "symptôme", "symptome", "حمى", "سعال", "تعب", "انفلونزا", "زكام", "gorge", "tête", "tete", "headache", "migraine", "mal de tête", "رأس", "راس", "صداع", "شقيقة", "بطن", "معدة", "ventre", "estomac", "abdomen", "stomach", "belly"],
    "neurologist": ["tête", "tete", "migraine", "mal de tête", "head", "headache", "رأس", "الرأس", "صداع", "شقيقة"],
    "gastro": ["ventre", "estomac", "abdomen", "stomach", "belly", "بطن", "البطن", "معدة", "المعدة"],
    "ortho": ["genou", "genoux", "articulation", "knee", "knees", "joint", "joints", "ركبة", "الركبة", "مفاصل", "المفاصل"]
}

URGENT_KEYWORDS = [
    # French
    "douleur thoracique", "respirer", "respiration", "perte de connaissance", "evanouissement", 
    "évanouissement", "saignement", "infarctus", "detresse", "détresse", "étouffer", "etouffer", "coma",
    # English
    "chest pain", "breathing", "breath", "unconscious", "fainting", "bleeding", "stroke", "heart attack", "choking",
    # Arabic
    "ألم في الصدر", "ضيق في التنفس", "صعوبة في التنفس", "فقدان الوعي", "إغماء", "نزيف", "جلطة"
]

MEDICATION_KEYWORDS = [
    # French
    "medicament", "médicament", "ordonnance", "prescrire", "prescris-moi", "traitement", "antibiotique", "doliprane", "ibuprofene", "ibuproféne", "ibuprofène", "aspirine", "paracetamol", "paracétamol", "quel medicament", "quel médicament", "prendre quoi",
    # English
    "medicine", "medication", "pill", "prescription", "prescribe", "treatment", "antibiotic", "painkiller", "paracetamol", "ibuprofen", "aspirin", "what pill", "what medicine",
    # Arabic
    "دواء", "أدوية", "علاج", "مضاد حيوي", "دوليبران", "وصفة طبية", "ايبوبروفين", "باراسيتامول", "اي دواء", "أي دواء"
]

VAGUE_REPLIES = {
    "fr": "Pouvez-vous préciser vos symptômes ou la zone concernée : tête, ventre, dos, dents, gorge, poitrine… ?",
    "en": "Could you please specify your symptoms or the affected area: head, stomach, back, teeth, throat, chest...?",
    "ar": "هل يمكنك تحديد أعراضك أو المنطقة المصابة: الرأس، البطن، الظهر، الأسنان، الحلق، الصدر...؟"
}

MEDICATION_REPLIES = {
    "fr": "Je ne peux pas conseiller de médicament ni de traitement. Je peux seulement vous orienter vers une spécialité médicale. Veuillez consulter un médecin ou un pharmacien.",
    "en": "I cannot recommend any medicine or treatment. I can only guide you to a medical specialty. Please consult a doctor or pharmacist.",
    "ar": "لا يمكنني وصف أدوية أو علاجات. يمكنني فقط توجيهك إلى التخصص الطبي المناسب. يرجى استشارة الطبيب أو الصيدلي."
}

TRANSLATIONS = {
    "fr": {
        "message": "Je ne peux pas poser de diagnostic, mais je peux vous orienter.",
        "warning": "Cet assistant ne remplace pas une consultation médicale. En cas d'urgence, contactez le 15 ou rendez-vous aux urgences.",
        "specialties": {
            "dentist": "Dentiste",
            "ent": "ORL",
            "cardiologist": "Cardiologue",
            "dermatologist": "Dermatologue",
            "rheumatologist": "Rhumatologue / Orthopédiste",
            "pediatrician": "Pédiatre",
            "ophthalmologist": "Ophtalmologue",
            "gynecologist": "Gynécologue",
            "gp": "Médecin généraliste",
            "neurologist": "Neurologue",
            "gastro": "Gastro-entérologue",
            "ortho": "Orthopédiste"
        }
    },
    "en": {
        "message": "I cannot make a medical diagnosis, but I can guide you to the right specialist.",
        "warning": "This assistant does not replace a medical consultation. In case of emergency, contact emergency services (911).",
        "specialties": {
            "dentist": "Dentist",
            "ent": "ENT Specialist",
            "cardiologist": "Cardiologist",
            "dermatologist": "Dermatologist",
            "rheumatologist": "Rheumatologist / Orthopedist",
            "pediatrician": "Pediatrician",
            "ophthalmologist": "Ophthalmologist",
            "gynecologist": "Gynecologist",
            "gp": "General Practitioner",
            "neurologist": "Neurologist",
            "gastro": "Gastroenterologist",
            "ortho": "Orthopedist"
        }
    },
    "ar": {
        "message": "لا يمكنني تقديم تشخيص طبي، ولكن يمكنني توجيهكم إلى التخصص المناسب.",
        "warning": "هذا المساعد لا يغني عن الاستشارة الطبية. في حالة الطوارئ، يرجى الاتصال بالإسعاف فوراً.",
        "specialties": {
            "dentist": "طبيب أسنان",
            "ent": "طبيب أنف وأذن وحنجرة",
            "cardiologist": "طبيب قلب",
            "dermatologist": "طبيب أمراض جلدية",
            "rheumatologist": "طبيب عظام ومفاصل",
            "pediatrician": "طبيب أطفال",
            "ophthalmologist": "طبيب عيون",
            "gynecologist": "طبيب أمراض نسائية",
            "gp": "طبيب عام",
            "neurologist": "طبيب أعصاب",
            "gastro": "طبيب جهاز هضمي",
            "ortho": "طبيب تقويم العظام"
        }
    }
}

def get_fallback_orientation(message: str, language: str, db_specialties: List[Any] = None) -> Dict[str, Any]:
    lang = language.lower() if language.lower() in TRANSLATIONS else "fr"
    msg_lower = message.lower().strip()
    
    # 1. Check for Medication/Treatment requests
    has_med = False
    for kw in MEDICATION_KEYWORDS:
        if kw in msg_lower:
            has_med = True
            break
            
    if has_med:
        return {
            "message": MEDICATION_REPLIES[lang],
            "specialties": [],
            "urgency": "normal",
            "warning": TRANSLATIONS[lang]["warning"],
            "needMoreInfo": False
        }

    # 2. Check for vague queries
    vague_patterns = [
        "j'ai mal", "je suis malade", "ça ne va pas", "ca ne va pas", "douleur", "aide moi", "aide-moi",
        "i am sick", "i'm sick", "pain", "hurt", "help me", "i feel bad",
        "أنا مريض", "انا مريض", "ألم", "الم", "ساعدني"
    ]
    if msg_lower in vague_patterns or len(msg_lower) < 6:
        return {
            "message": VAGUE_REPLIES[lang],
            "specialties": [],
            "urgency": "normal",
            "warning": TRANSLATIONS[lang]["warning"],
            "needMoreInfo": True
        }
        
    # 3. Check for Specialties match
    KEY_TO_DB_CODE = {
        "dentist": "CHIRURGIE_DENTAIRE",
        "ent": "ORL",
        "cardiologist": "CARDIOLOGIE",
        "dermatologist": "DERMATOLOGIE",
        "rheumatologist": "RHUMATOLOGIE",
        "pediatrician": "PEDIATRIE",
        "ophthalmologist": "OPHTALMOLOGIE",
        "gynecologist": "GYNECOLOGIE",
        "gp": "MEDECINE_GENERALE",
        "neurologist": "NEUROLOGIE",
        "gastro": "GASTROENTEROLOGIE",
        "ortho": "CHIRURGIE_GENERALE"
    }

    available_map = {s.code: s for s in db_specialties} if db_specialties else {}
    
    recommended_specs = []
    seen_codes = set()
    for spec_key, keywords in SPECIALTIES_KEYWORDS.items():
        db_code = KEY_TO_DB_CODE.get(spec_key)
        if not db_code or db_code not in available_map:
            continue
        if db_code in seen_codes:
            continue
            
        for kw in keywords:
            if kw in msg_lower:
                db_item = available_map[db_code]
                label = TRANSLATIONS[lang]["specialties"].get(spec_key) or db_item.libelle
                recommended_specs.append({
                    "id": db_item.id,
                    "code": db_item.code,
                    "label": label
                })
                seen_codes.add(db_code)
                break
                
    # 4. Determine Urgency
    urgency = "normal"
    for kw in URGENT_KEYWORDS:
        if kw in msg_lower:
            urgency = "urgent"
            break
            
    is_chest_coeur = any(c in msg_lower for c in ["poitrine", "coeur", "cœur", "chest", "heart", "صدر", "قلب"])
    is_pain = any(p in msg_lower for p in ["douleur", "mal", "forte", "fort", "pain", "hurt", "strong", "ألم", "شديد", "شديدة", "وجع"])
    if is_chest_coeur and is_pain:
        urgency = "urgent"
            
    # 5. Handle result or vague input
    recommended_specs = recommended_specs[:3]
    if recommended_specs:
        return {
            "message": TRANSLATIONS[lang]["message"],
            "specialties": recommended_specs,
            "urgency": urgency,
            "warning": TRANSLATIONS[lang]["warning"],
            "needMoreInfo": False
        }
    else:
        # Unmatched input is considered too vague to be useful
        return {
            "message": VAGUE_REPLIES[lang],
            "specialties": [],
            "urgency": "normal",
            "warning": TRANSLATIONS[lang]["warning"],
            "needMoreInfo": True
        }

def reconstruct_message(last_user_msg: str, current_msg: str, lang: str) -> str:
    last = last_user_msg.strip().lower()
    curr = current_msg.strip().lower()
    
    # Clean French combinations
    if lang == "fr":
        if "tête" in curr or "tete" in curr:
            return "j'ai mal à la tête" if "mal" in last else f"{last_user_msg} tête"
        elif "ventre" in curr or "estomac" in curr or "abdomen" in curr:
            return "j'ai mal au ventre" if "mal" in last else f"{last_user_msg} ventre"
        elif "dos" in curr:
            return "j'ai mal au dos" if "mal" in last else f"{last_user_msg} dos"
        elif "dent" in curr:
            return "j'ai mal aux dents" if "mal" in last else f"{last_user_msg} dents"
        elif "gorge" in curr:
            return "j'ai mal à la gorge" if "mal" in last else f"{last_user_msg} gorge"
        elif "poitrine" in curr or "coeur" in curr or "cœur" in curr:
            if last == "douleur":
                return "douleur poitrine"
            return f"{last_user_msg} à la poitrine" if "mal" in last or "douleur" in last else f"{last_user_msg} poitrine"
        elif "yeux" in curr or "oeil" in curr or "œil" in curr:
            return "j'ai mal aux yeux" if "mal" in last else f"{last_user_msg} yeux"
        elif "peau" in curr:
            return "j'ai mal à la peau" if "mal" in last else f"{last_user_msg} peau"
        elif "genou" in curr or "articulation" in curr:
            return "j'ai mal au genou" if "mal" in last else f"{last_user_msg} genou"
        elif "oreille" in curr:
            return "j'ai mal à l'oreille" if "mal" in last else f"{last_user_msg} oreille"
            
    # Clean English combinations
    elif lang == "en":
        if "head" in curr or "migraine" in curr:
            return "i have a headache" if "pain" in last or "hurt" in last else f"{last_user_msg} head"
        elif "stomach" in curr or "belly" in curr or "abdomen" in curr:
            return "i have a stomach ache" if "pain" in last or "hurt" in last else f"{last_user_msg} stomach"
        elif "back" in curr:
            return "i have back pain" if "pain" in last or "hurt" in last else f"{last_user_msg} back"
        elif "teeth" in curr or "tooth" in curr:
            return "i have a toothache" if "pain" in last or "hurt" in last else f"{last_user_msg} teeth"
        elif "throat" in curr:
            return "i have a sore throat" if "pain" in last or "hurt" in last else f"{last_user_msg} throat"
        elif "chest" in curr or "heart" in curr:
            return "chest pain" if "pain" in last or "hurt" in last else f"{last_user_msg} chest"
        elif "eye" in curr:
            return "i have eye pain" if "pain" in last or "hurt" in last else f"{last_user_msg} eyes"
        elif "skin" in curr:
            return "i have a skin problem"
        elif "knee" in curr or "joint" in curr:
            return "i have joint pain"
        elif "ear" in curr:
            return "i have an earache"
            
    # Clean Arabic combinations
    elif lang == "ar":
        if "رأس" in curr or "راس" in curr:
            return "عندي ألم في الرأس" if "ألم" in last or "مرض" in last else f"{last_user_msg} الرأس"
        elif "بطن" in curr or "معدة" in curr or "معده" in curr:
            return "عندي ألم في البطن" if "ألم" in last or "مرض" in last else f"{last_user_msg} البطن"
        elif "ظهر" in curr:
            return "عندي ألم في الظهر" if "ألم" in last or "مرض" in last else f"{last_user_msg} الظهر"
        elif "أسنان" in curr or "اسنان" in curr or "سن" in curr or "ضرس" in curr:
            return "عندي ألم في الأسنان" if "ألم" in last or "مرض" in last else f"{last_user_msg} الأسنان"
        elif "حلق" in curr or "حنجرة" in curr or "حنجره" in curr:
            return "عندي ألم في الحلق" if "ألم" in last or "مرض" in last else f"{last_user_msg} الحلق"
        elif "صدر" in curr or "قلب" in curr:
            return "ألم في الصدر" if "ألم" in last or "مرض" in last else f"{last_user_msg} الصدر"
        elif "عين" in curr or "عيون" in curr:
            return "عندي ألم في العين" if "ألم" in last or "مرض" in last else f"{last_user_msg} العيون"
        elif "جلد" in curr or "بشرة" in curr or "بشره" in curr:
            return "عندي مشكلة في الجلد"
        elif "ركبة" in curr or "ركبه" in curr or "مفاصل" in curr:
            return "عندي ألم في المفاصل"
        elif "أذن" in curr or "إذن" in curr or "اذن" in curr:
            return "عندي ألم في الأذن"
            
    return f"{last_user_msg} {current_msg}"
