from fastapi import FastAPI, File, UploadFile
from fastapi.responses import JSONResponse
import whisper
import os
import uuid

app = FastAPI()


@app.post("/transcribe")
async def transcribe(file: UploadFile = File(...)):
  # Whisper 모델은 요청 시 로딩 (로컬 캐시 있음)
  model = whisper.load_model("base")

  # 업로드된 오디오 파일을 임시로 저장
  file_extension = os.path.splitext(file.filename)[1]
  temp_filename = f"temp_{uuid.uuid4()}{file_extension}"

  with open(temp_filename, "wb") as f:
    content = await file.read()
    f.write(content)

  # Whisper로 텍스트 추출
  result = model.transcribe(temp_filename)

  # 텍스트만 추출해서 반환
  response = {
    "transcript": result.get("text", ""),
    "language": result.get("language", "unknown")
  }

  os.remove(temp_filename)  # 임시 파일 삭제
  return JSONResponse(content=response)
