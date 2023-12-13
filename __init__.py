import os
import uuid

os.environ["SECRET_KEY"] = str(uuid.uuid4())
os.environ["ALGORITHM"] = "HS256"

