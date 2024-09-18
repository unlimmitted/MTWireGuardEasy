Front : https://github.com/unlimmitted/MTWireGuardEasy-frontend

MikroTik system requirements: 
1. Only RoterBOARD
2. 500 bytes of free space in the root
3. RouterOS not lower 7.15 

```cpp
docker build --tag mtwgeasy .
```

```cpp
docker run --name MTWGEasy -p 8080:8080 -e GATEWAY=<you-mikrotik-ip> -e MIKROTIK_USER=<you-mikrotik-admin-login> -e MIKROTIK_PASSWORD=<you-mikrotik-pass>
```

![main](https://github.com/user-attachments/assets/d48084ba-789b-4e79-95a7-631bc4d40fab)

![peerModal](https://github.com/user-attachments/assets/578e0438-1879-4757-8443-76f33079d9eb)
