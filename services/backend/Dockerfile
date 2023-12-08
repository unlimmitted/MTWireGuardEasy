FROM python:3.11
RUN mkdir /code
RUN apt-get git
RUN git pull
WORKDIR /code
COPY ./requirements.txt /code/requirements.txt
RUN pip install --upgrade pip
RUN pip install --no-cache-dir --upgrade -r /code/requirements.txt
COPY . .

WORKDIR app
CMD gunicorn -k uvicorn.workers.UvicornWorker main:app --bind=0.0.0.0:8000
