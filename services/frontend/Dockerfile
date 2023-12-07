FROM node:lts-alpine

WORKDIR /app
RUN apk add --no-cache git
RUN git clone https://github.com/unlimmitted/MTWireGuardEasy-frontend.git

WORKDIR /app/MTWireGuardEasy-frontend/
COPY package*.json ./

RUN npm install http-server
RUN npm install vite
RUN npm install

COPY . .

RUN npm run build

EXPOSE 8080
CMD [ "http-server", "dist" ]