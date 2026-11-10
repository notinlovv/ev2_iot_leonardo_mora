# IoT SafeGuard OT - Sistema de Alerta Temprana y Mitigación Pasiva

Aplicación móvil Android para la supervisión y mitigación de fugas de gas, acumulación de humo y conatos de incendio en entornos OT/industriales. Proyecto desarrollado para la Asignatura **Aplicaciones Móviles para IoT** (Unidad 2).

## Hardware y Actuadores Integrados (Evaluación 1)
- **Sensor MQ-2:** Detección de gas licuado/combustibles y humo por umbrales PPM.
- **Sensor KY-026:** Detección óptica de llama/fuego directo (radiación IR).
- **Semáforo Virtual (RGB):** Verde (Gas), Amarillo (Humo), Rojo (Fuego crítico).
- **Alerta Acústica (Buzzer KY-006):** 1 pitido (Gas), 2 pitidos (Humo), 3 pitidos (Fuego).
- **Ventilador 4010:** Mecanismo de mitigación pasiva forzada (control manual y automático).

## Arquitectura y Conectividad (Unidad 2)
- **Red Inalámbrica:** Comunicación Wi-Fi local mediante Sockets TCP (puerto 8888) en arquitectura Cliente-Servidor (Dispositivo A: Gateway Simulador OT / Dispositivo B: Terminal Operador).
- **Persistencia Local:** SQLite mediante AndroidX Room (`AppDatabase`) con tablas para usuarios (`users`) y bitácora de eventos (`telemetry_logs`).

## Ciberseguridad IoT (ISO/IEC 27400:2022)
- **Cifrado en Reposo:** Hashing criptográfico SHA-256 para almacenamiento de credenciales en base de datos.
- **Manejo de Sesión Segura:** Implementación de `EncryptedSharedPreferences` con claves maestras AES-256 GCM.
- **Mínimo Privilegio:** Restricción estricta de permisos en `AndroidManifest.xml` (`INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`).

## Instrucciones de Ejecución
1. Instalar el APK en dos dispositivos en la misma red Wi-Fi (o emulador + teléfono físico).
2. En el primer dispositivo: Iniciar sesión y entrar a **Modo Servidor / Simulador OT**. Tomar nota de la IP Wi-Fi mostrada.
3. En el segundo dispositivo: Iniciar sesión, ingresar al **Modo Dashboard Operador**, colocar la IP del primer dispositivo y presionar **Conectar**.
