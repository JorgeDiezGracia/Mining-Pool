# Mining Pool

Proyecto de la 1ª Evaluación de la asignatura de Programación de Servicios y Procesos.
Consiste en simular un pool de minería de criptomonedas con un servidor central y varios clientes mineros.

## ¿Cómo funciona?

## Arrancar el proyecto

**1. Servidor**

Ejecutar la clase `MiningServerApp`. 

**2. Cliente**

Ejecutar la clase `MiningClientApp`. 


## Estructura del proyecto

```
src/main/java/
├── client/
│   ├── MiningClient.java            # Cliente por consola (sin UI)
│   ├── MiningClientApp.java         # Entrada JavaFX del cliente
│   └── MiningClientController.java  # Controlador de la UI del cliente
├── common/
│   └── Protocol.java                # Constantes del protocolo
└── server/
    ├── BlockGenerator.java          # Genera bloques con transacciones falsas
    ├── ClientHandler.java           # Hilo por cada cliente conectado
    ├── MiningServer.java            # Lógica principal del servidor
    ├── MiningServerApp.java         # Entrada JavaFX del servidor
    └── MiningServerController.java  # Controlador de la UI del servidor
```
## Funcionalidades obligatorias ✅

- Generación de bloques con transacciones aleatorias (origen, destino, cantidad)
- Clientes pueden conectarse y desconectarse; el servidor lleva la lista de conexiones activas
- El servidor gestiona las conexiones de forma concurrente (un hilo por cliente)
- Los clientes aceptan peticiones de minado, buscan el salt y envían la solución al servidor
- El servidor valida la solución y finaliza el proceso en el resto de clientes

## Funcionalidades opcionales ✅

- **UI para clientes** — ventana JavaFX con log de mensajes, barra de progreso y salt encontrado
- **Minado concurrente en el cliente** — pool de hilos que divide el rango asignado y mina en paralelo
- **UI para el servidor** — ventana JavaFX con lista de clientes conectados, bloque actual, últimas soluciones y log completo
