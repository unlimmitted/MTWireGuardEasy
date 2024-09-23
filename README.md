**Frontend : https://github.com/unlimmitted/MTWireGuardEasy-frontend**

MikroTik system requirements: 
1. Only **_RouterBoard_**
2. **_2 Kb_** of free space
3. RouterOS **_not lower 7.15_** 

## **Launch command**
```cpp
docker build --tag mtwgeasy . && docker run --name MTWGEasy \
 -d --no-cache -p 8080:8080 \
 -e GATEWAY=<you-mikrotik-ip> \
 -e MIKROTIK_USER=<you-mikrotik-admin-login> \
 -e MIKROTIK_PASSWORD=<you-mikrotik-pass> \
 mtwgeasy
```

## **Settings**
#### **Only wireguard server**

#### **VPN chain mode**

## **Appearance:**

###### _Main screen_
![Скриншот сделанный 2024-09-21 в 22 30 11](https://github.com/user-attachments/assets/0ef41b8a-57da-4c79-8c8c-ae82245f43ed)

###### **Peer modal**
![peerModal](https://github.com/user-attachments/assets/578e0438-1879-4757-8443-76f33079d9eb)

###### **Settings screen**
