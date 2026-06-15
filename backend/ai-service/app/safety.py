from typing import Dict, Any

def sanitize_response(data: dict) -> dict:
    """
    Sanitizes and enforces format rules on the response dict.
    Makes sure we have all keys, limits specialties to 1-3 items.
    Allows empty list if needMoreInfo is True or for medication responses.
    """
    # needMoreInfo validation
    if "needMoreInfo" in data:
        data["needMoreInfo"] = bool(data["needMoreInfo"])
    else:
        data["needMoreInfo"] = False

    # Specialties validation
    if "specialties" in data:
        if not isinstance(data["specialties"], list):
            data["specialties"] = [data["specialties"]]
        
        # Clean and sanitize specialties
        cleaned_specialties = []
        for s in data["specialties"]:
            if isinstance(s, dict):
                if "code" in s:
                    cleaned_specialties.append({
                        "id": s.get("id") or 0,
                        "code": s.get("code"),
                        "label": s.get("label") or s.get("code")
                    })
            elif isinstance(s, str):
                cleaned_specialties.append({
                    "id": 0,
                    "code": s,
                    "label": s
                })
        
        # Propose only 1 to 3 specialties
        data["specialties"] = cleaned_specialties[:3]
    else:
        data["specialties"] = []

    # If needMoreInfo is True, specialties must be empty
    if data["needMoreInfo"]:
        data["specialties"] = []
        
    # Urgency validation
    if "urgency" in data:
        if data["urgency"] not in ["normal", "urgent"]:
            data["urgency"] = "normal"
    else:
        data["urgency"] = "normal"
        
    # Default fallback message & warning if missing
    if "message" not in data or not data["message"]:
        data["message"] = "Je ne peux pas poser de diagnostic, mais je peux vous orienter."
        
    if "warning" not in data or not data["warning"]:
        data["warning"] = "Cet assistant ne remplace pas une consultation médicale. En cas d'urgence, contactez les urgences."
        
    return data
