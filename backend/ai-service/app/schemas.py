from pydantic import BaseModel, Field, field_validator
from typing import List

class ChatMessage(BaseModel):
    role: str = Field(..., description="Role of the sender ('user' or 'assistant')")
    content: str = Field(..., description="Message text")

class SpecialtyDTO(BaseModel):
    id: int
    code: str
    libelle: str

class SpecialtyRecommendation(BaseModel):
    id: int
    code: str
    label: str

class OrientationRequest(BaseModel):
    message: str = Field(..., min_length=3, max_length=500, description="Symptom description from the user")
    language: str = Field("fr", description="Requested language ('fr', 'en', 'ar')")
    history: List[ChatMessage] = Field(default=[], description="Conversation history")
    specialties: List[SpecialtyDTO] = Field(default=[], description="Available specialties from database")

    @field_validator("language")
    @classmethod
    def validate_language(cls, value: str) -> str:
        val = value.lower().strip()
        if val not in ["fr", "en", "ar"]:
            raise ValueError("Language must be 'fr', 'en', or 'ar'")
        return val

class OrientationResponse(BaseModel):
    message: str = Field(..., description="Reassuring explanation in the requested language")
    specialties: List[SpecialtyRecommendation] = Field(..., description="Recommended medical specialties (1-3)")
    urgency: str = Field("normal", description="Urgency level ('normal' or 'urgent')")
    warning: str = Field(..., description="Disclaimer message in the requested language")
    needMoreInfo: bool = Field(False, description="Flag indicating if the assistant needs more specific details")
