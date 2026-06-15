import os
import logging
from fastapi import FastAPI, HTTPException, status
from fastapi.middleware.cors import CORSMiddleware
from app.schemas import OrientationRequest, OrientationResponse
from app.orientation_service import get_orientation

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s"
)
logger = logging.getLogger(__name__)

app = FastAPI(
    title="MedConnect AI Orientation Service",
    description="Python FastAPI Microservice to pre-orient patient symptoms to medical specialties.",
    version="1.0.0"
)

# Enable CORS for local testing
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.on_event("startup")
def startup_event():
    gemini_key = os.environ.get("GEMINI_API_KEY")
    if not gemini_key or gemini_key.strip() == "" or "PUT_YOUR_GEMINI_API_KEY" in gemini_key:
        logger.warning("GEMINI_API_KEY is not defined or is placeholder. Service will run in fallback rule-based mode.")
    else:
        logger.info("GEMINI_API_KEY is configured. Gemini AI orientation is active.")

@app.get("/health", status_code=status.HTTP_200_OK)
def health_check():
    """
    Health check endpoint for Docker container checks.
    """
    gemini_key = os.environ.get("GEMINI_API_KEY")
    has_gemini = bool(gemini_key and gemini_key.strip() != "" and "PUT_YOUR_GEMINI_API_KEY" not in gemini_key)
    return {
        "status": "UP",
        "service": "ai-service",
        "gemini_active": has_gemini
    }

@app.post("/api/ai/orientation", response_model=OrientationResponse, status_code=status.HTTP_200_OK)
async def ai_orientation(request: OrientationRequest):
    """
    Pre-orients patients based on their symptoms.
    """
    try:
        response = await get_orientation(request.message, request.language, request.history, request.specialties)
        return response
    except Exception as e:
        logger.error(f"Error handling orientation request: {type(e).__name__}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="An unexpected error occurred while processing symptoms."
        )
