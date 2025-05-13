from fastapi import FastAPI, File, UploadFile, Form, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from PIL import Image
import io
import torch
from transformers import BlipProcessor, BlipForConditionalGeneration, DetrImageProcessor, DetrForObjectDetection
import gc
import os
import logging
from dotenv import load_dotenv
from typing import List, Optional

# Load environment variables
load_dotenv()

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="Image Analysis API")

# Configure CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Initialize models with device configuration
device = "cuda" if torch.cuda.is_available() else "cpu"
logger.info(f"Using device: {device}")

# Global variables for models
processor = None
model = None
detr_processor = None
detr_model = None

def load_models():
    global processor, model, detr_processor, detr_model
    try:
        logger.info("Loading BLIP model...")
        processor = BlipProcessor.from_pretrained("Salesforce/blip-image-captioning-base")
        model = BlipForConditionalGeneration.from_pretrained("Salesforce/blip-image-captioning-base").to(device)
        model.eval()

        logger.info("Loading DETR model...")
        detr_processor = DetrImageProcessor.from_pretrained("facebook/detr-resnet-50")
        detr_model = DetrForObjectDetection.from_pretrained("facebook/detr-resnet-50").to(device)
        detr_model.eval()

        # Free up memory
        gc.collect()
        if device == "cuda":
            torch.cuda.empty_cache()
        logger.info("Models loaded successfully!")
    except Exception as e:
        logger.error(f"Error loading models: {str(e)}")
        raise

@app.on_event("startup")
async def startup_event():
    logger.info("Starting up application...")
    load_models()
    logger.info("Application startup complete!")

def process_prompt(prompt: str) -> str:
    if not prompt:
        return "describe what you see in this image"
    
    prompt = prompt.lower()
    if "what" in prompt and "this" in prompt:
        return prompt
    elif "what" in prompt:
        return f"what is this {prompt.replace('what', '').strip()}"
    else:
        return f"what is this {prompt}"

@app.post("/api/detect")
async def detect_objects(
    prompt: str = Form(None),
    width: int = Form(...),
    height: int = Form(...),
    image: UploadFile = File(...)
):
    try:
        logger.info(f"Received request - Prompt: {prompt}, Width: {width}, Height: {height}")
        
        if processor is None or model is None or detr_processor is None or detr_model is None:
            logger.info("Models not loaded, loading now...")
            load_models()

        # Read and process image
        contents = await image.read()
        logger.info(f"Received image size: {len(contents)} bytes")
        
        if len(contents) > 10 * 1024 * 1024:  # 10MB limit
            raise HTTPException(status_code=400, detail="Image too large")
            
        image = Image.open(io.BytesIO(contents))
        logger.info(f"Image opened successfully - Size: {image.size}")
        
        # Resize image if too large
        max_size = 800
        if max(image.size) > max_size:
            ratio = max_size / max(image.size)
            new_size = tuple(int(dim * ratio) for dim in image.size)
            image = image.resize(new_size, Image.Resampling.LANCZOS)
            logger.info(f"Image resized to: {new_size}")
        
        # Object detection
        logger.info("Starting object detection...")
        with torch.no_grad():
            inputs = detr_processor(images=image, return_tensors="pt").to(device)
            outputs = detr_model(**inputs)
            
            target_sizes = torch.tensor([image.size[::-1]])
            results = detr_processor.post_process_object_detection(outputs, target_sizes=target_sizes, threshold=0.9)[0]
            
            detected_objects = []
            for score, label, box in zip(results["scores"], results["labels"], results["boxes"]):
                if score > 0.9:
                    detected_objects.append({
                        "label": detr_model.config.id2label[label.item()],
                        "score": score.item(),
                        "box": box.tolist()
                    })
        
        logger.info(f"Detected objects: {detected_objects}")
        
        # Clear memory
        del inputs, outputs, results
        gc.collect()
        if device == "cuda":
            torch.cuda.empty_cache()
        
        # Process prompt
        processed_prompt = process_prompt(prompt)
        logger.info(f"Processed prompt: {processed_prompt}")
        
        # Generate caption
        logger.info("Generating caption...")
        with torch.no_grad():
            inputs = processor(image, processed_prompt, return_tensors="pt").to(device)
            outputs = model.generate(
                **inputs,
                max_length=50,
                num_beams=5,
                temperature=0.7,
                do_sample=True
            )
            caption = processor.decode(outputs[0], skip_special_tokens=True)
        
        logger.info(f"Generated caption: {caption}")
        
        # Clear memory again
        del inputs, outputs
        gc.collect()
        if device == "cuda":
            torch.cuda.empty_cache()
        
        # Convert detected objects to the expected format
        results = []
        for obj in detected_objects:
            box = obj["box"]
            # Convert coordinates to integers to avoid floating point issues
            results.append({
                "label": obj["label"],
                "coordinates": [
                    int(box[1]),  # y1
                    int(box[0]),  # x1
                    int(box[3]),  # y2
                    int(box[2])   # x2
                ]
            })
        
        # Clean up the caption by removing the prompt text
        cleaned_caption = caption
        if prompt:
            # Remove the prompt text from the caption
            cleaned_caption = caption.replace(processed_prompt, "").strip()
            # Remove any remaining brackets and their contents
            cleaned_caption = cleaned_caption.replace("[", "").replace("]", "").strip()
        
        response = {
            "result": results,
            "error": None,
            "response": f"Detected objects: {', '.join(obj['label'] for obj in detected_objects)}. {cleaned_caption}",
            "labels": [],
            "polygons": []
        }
        logger.info(f"Sending response: {response}")
        return response
        
    except Exception as e:
        logger.error(f"Error processing request: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/")
async def root():
    return {"message": "Image Analysis API is running"}

@app.get("/health")
async def health_check():
    return {"status": "healthy"}

if __name__ == "__main__":
    import uvicorn
    port = int(os.getenv("PORT", 10000))
    uvicorn.run(app, host="0.0.0.0", port=port, log_level="info") 