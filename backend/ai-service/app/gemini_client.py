import os
import httpx
import json
import logging
from typing import Dict, Any, Optional, List

logger = logging.getLogger(__name__)

GEMINI_API_KEY = os.environ.get("GEMINI_API_KEY")

SYSTEM_PROMPT_TEMPLATE = """Tu es un assistant de pré-orientation médicale pour la plateforme MedConnect.
Tu ne fais pas de diagnostic médical.
Tu ne prescris aucun médicament ni traitement.
Tu réponds STRICTEMENT sous format JSON.
Tu rappelles toujours qu'un médecin est nécessaire et que ton aide ne remplace pas une consultation.

Le format de réponse JSON attendu doit posséder STRICTEMENT ces clés :
- message: un message rassurant, neutre et poli rédigé dans la langue demandée.
- specialties: une liste (array) de 1 à 3 spécialités recommandées parmi la liste disponible. Chaque élément de cet array doit être un objet JSON avec STRICTEMENT ces clés :
  * id: l'identifiant numérique de la spécialité (id)
  * code: le code unique de la spécialité (code)
  * label: le nom de la spécialité traduit dans la langue demandée (ex: si langue='en' et code='MEDECINE_GENERALE', traduis par 'General Practitioner', si langue='ar', traduis par 'طبيب عام', si langue='fr' ou autre, traduis par 'Médecine générale' ou 'Médecin généraliste').
- urgency: 'normal' ou 'urgent' (à mettre à 'urgent' en cas de symptômes graves évoquant une urgence vitale comme douleur thoracique violente, détresse respiratoire aiguë, hémorragie massive, perte de connaissance, paralysie soudaine).
- warning: une mise en garde médicale indiquant qu'en cas d'urgence il faut contacter les secours, rédigée dans la langue demandée.
- needMoreInfo: un booléen (true/false) indiquant si le message de l'utilisateur est trop vague pour proposer des spécialités.

Règles impératives :
1. Tu ne dois choisir que parmi la liste des spécialités disponibles fournie ci-dessous. Tu ne dois proposer AUCUN code de spécialité en dehors de cette liste.
2. N'utilise pas automatiquement 'MEDECINE_GENERALE' pour toutes les entrées inconnues ou floues. N'utilise 'MEDECINE_GENERALE' que s'il y a des symptômes généraux médicalement logiques (fièvre, fatigue, syndrome grippal, courbatures, etc.).
3. Si le message de l'utilisateur est trop vague (ex: "j'ai mal", "je suis malade", "ça ne va pas", "douleur", "aide moi", "je me sens fatigué sans raison"), ou ne correspond à aucune spécialité de la liste, tu dois :
   - Mettre needMoreInfo à true
   - Mettre specialties à []
   - Demander poliment de préciser la zone concernée ou les symptômes (ex: "Pouvez-vous préciser la zone concernée : tête, ventre, dos, dents, gorge, poitrine… ?") dans le champ message.
4. Si l'utilisateur demande des médicaments, traitements ou ordonnances (ex: "donne-moi un médicament", "quel antibiotique prendre"), tu dois :
   - Mettre needMoreInfo à false
   - Mettre specialties à []
   - Mettre urgency à 'normal'
   - Refuser poliment dans le champ message en rappelant de conseiller seulement l'orientation vers une spécialité médicale et d'inviter à consulter un médecin ou pharmacien.
5. Si les symptômes sont clairs et correspondent à une ou plusieurs spécialités disponibles, propose-les (maximum 3), et mets needMoreInfo à false.

Voici la liste des spécialités médicales disponibles (id, code, libelle français) :
{specialties_list}

Tu dois répondre exclusivement en JSON valide, sans bloc de code Markdown (ne pas entourer de ```json ... ```), juste l'objet JSON brut.
"""

async def query_gemini(message: str, language: str, db_specialties: List[Any] = None) -> Optional[Dict[str, Any]]:
    # Secure API key check (must be set and not the default placeholder)
    if not GEMINI_API_KEY or GEMINI_API_KEY.strip() == "" or "PUT_YOUR_GEMINI_API_KEY" in GEMINI_API_KEY:
        logger.warning("GEMINI_API_KEY is not configured or uses a placeholder. Skipping Gemini API call.")
        return None
        
    specialties_str = ""
    if db_specialties:
        specialties_str = "\n".join([f"- id: {s.id}, code: '{s.code}', libelle: '{s.libelle}'" for s in db_specialties])
    else:
        specialties_str = "- (Aucune spécialité disponible dans la base)"

    system_prompt = SYSTEM_PROMPT_TEMPLATE.format(specialties_list=specialties_str)
    prompt = f"{system_prompt}\n\nLangue demandée : {language}\nMessage du patient : {message}"
    
    # We never log the complete message or the API key
    logger.info(f"Calling Gemini API for language: {language} (query length: {len(message)}, specialties count: {len(db_specialties or [])})")
    
    url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key={GEMINI_API_KEY}"
    payload = {
        "contents": [
            {
                "parts": [
                    {"text": prompt}
                ]
            }
        ],
        "generationConfig": {
            "responseMimeType": "application/json",
            "temperature": 0.2
        }
    }
    
    async with httpx.AsyncClient() as client:
        try:
            # Short timeout to ensure responsiveness (8 seconds max)
            response = await client.post(url, json=payload, timeout=8.0)
            
            if response.status_code != 200:
                logger.error(f"Gemini API returned status code {response.status_code}. Details omitted for safety.")
                return None
                
            result = response.json()
            candidates = result.get("candidates", [])
            if not candidates:
                logger.error("No candidates returned in Gemini API response.")
                return None
                
            text_response = candidates[0]["content"]["parts"][0]["text"]
            text_response = text_response.strip()
            
            # Clean possible markdown wrapping
            if text_response.startswith("```"):
                lines = text_response.splitlines()
                if lines[0].startswith("```"):
                    lines = lines[1:]
                if lines[-1].startswith("```"):
                    lines = lines[:-1]
                text_response = "\n".join(lines).strip()
                
            data = json.loads(text_response)
            
            # Map of available specialties by code
            available_map = {s.code: s for s in db_specialties} if db_specialties else {}
            
            validated_specs = []
            for spec in data.get("specialties", []):
                if isinstance(spec, dict) and "code" in spec:
                    code = spec["code"].strip().upper()
                    if code in available_map:
                        db_item = available_map[code]
                        label = spec.get("label") or db_item.libelle
                        validated_specs.append({
                            "id": db_item.id,
                            "code": db_item.code,
                            "label": label
                        })
                elif isinstance(spec, str):
                    code_to_find = spec.strip().upper()
                    if code_to_find in available_map:
                        db_item = available_map[code_to_find]
                        validated_specs.append({
                            "id": db_item.id,
                            "code": db_item.code,
                            "label": db_item.libelle
                        })
            
            data["specialties"] = validated_specs
            
            # If specialties is empty but needMoreInfo is false AND they didn't request medicine,
            # let's set needMoreInfo to True and request more details.
            has_med_words = any(kw in message.lower() for kw in ["medicament", "médicament", "ordonnance", "traitement", "medicine", "pill", "دواء", "علاج"])
            if not data.get("specialties") and not data.get("needMoreInfo") and not has_med_words:
                data["needMoreInfo"] = True
                if language.lower() == "ar":
                    data["message"] = "هل يمكنك تحديد أعراضك أو المنطقة المصابة بمزيد من التفصيل؟"
                elif language.lower() == "en":
                    data["message"] = "Could you please provide more details about your symptoms or the affected area?"
                else:
                    data["message"] = "Pouvez-vous préciser vos symptômes ou la zone concernée pour nous aider à mieux vous orienter ?"

            logger.info("Successfully received and validated response from Gemini.")
            return data
            
        except httpx.TimeoutException:
            logger.error("Timeout occurred while contacting Gemini API.")
            return None
        except httpx.RequestError as e:
            logger.error(f"Network request to Gemini failed: {type(e).__name__}")
            return None
        except (json.JSONDecodeError, KeyError, IndexError) as e:
            logger.error(f"Failed to parse or validate Gemini JSON: {type(e).__name__}")
            return None
