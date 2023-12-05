import uuid
import os
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from passlib.context import CryptContext

os.environ["SECRET_KEY"] = str(uuid.uuid4())
os.environ["ALGORITHM"] = "HS256"

from view.api_paths import *

app = FastAPI()

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

origins = ["*"]

app.include_router(router, prefix="/api/v1")

app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_methods=["*"],
    allow_headers=["*"],
)
