# MTWireGuardEasy

![logo](https://github.com/unlimmitted/MTWireGuardEasy/assets/108941648/7b2ae56c-4649-44d3-91c8-9036621109f3)


Cloning
```console
git clone https://github.com/unlimmitted/MTWireGuardEasy.git
```

Build
```console
sudo docker build --tag mtwgeasy .
```

Run
```console
sudo docker run --name MTWireGuardEasy -d --restart unless-stopped -p 5000:5000 mtwgeasy
```

Frontend: https://github.com/unlimmitted/MTWireGuardEasy-frontend
