# Especificación de producto — AparcaMálaga PMR

## Propósito
Ayudar a localizar plazas de aparcamiento reservadas para personas con movilidad reducida en Málaga y abrir una aplicación de navegación hasta la plaza elegida.

El uso inicial es personal, principalmente para ayudar a la suegra de Javi. La publicación general queda fuera del alcance inicial.

## Requisitos vinculantes
- Aplicación Android nativa en Kotlin.
- Interfaz móvil con Jetpack Compose.
- Compatibilidad con Android Auto mediante Android for Cars App Library.
- Categoría de coche `androidx.car.app.category.POI`; no usar la categoría obsoleta `PARKING`.
- Datos procedentes de la fuente abierta oficial del Ayuntamiento de Málaga.
- No inventar ni incluir ubicaciones de ejemplo como si fueran plazas reales.
- Una ubicación conocida no implica disponibilidad en tiempo real; comunicarlo claramente.
- Delegar la navegación giro a giro a una aplicación de navegación instalada.
- Sin cuentas, servidor propio, publicidad ni analítica en el alcance inicial.
- Mantener una copia local de los datos para conexión deficiente y limitar las actualizaciones.
- Solicitar sólo los permisos de ubicación estrictamente necesarios y en contexto.

## Arquitectura objetivo
- Capa de dominio y datos compartida por móvil y coche.
- Room para caché local.
- Cliente de la fuente municipal pendiente de definir tras inspeccionar su formato y licencia.
- UI móvil Compose.
- `CarAppService` con plantillas aprobadas para Android Auto.

## Primera versión funcional prevista
### Teléfono
- Listado de plazas.
- Búsqueda por zona o dirección.
- Escritura fluida: índice del catálogo y filtrado fuera del hilo de interfaz; cancelar búsquedas anteriores.
- Si una dirección no coincide con plazas, resolverla en Málaga y mostrar las plazas municipales más cercanas a ella, indicando el origen de las distancias. No sustituir una dirección desconocida por una ubicación inventada.
- Al ordenar por cercanía, mostrar una ubicación reciente válida mientras se solicita una nueva por red y GPS con espera limitada.
- Orden por distancia cuando haya permiso de ubicación.
- Detalle, fecha de actualización y atribución de la fuente.
- Acción para abrir navegación externa.

### Android Auto
- Pantalla principal de tarjetas en `PlaceListMapTemplate`, con búsqueda de direcciones accesible desde la lista y detalle al seleccionar una plaza.
- Sin pantalla propia de seguimiento del vehículo, cámara animada ni navegación interna. El mapa del host que acompaña a las tarjetas sólo sitúa las plazas; la navegación se delega a la aplicación externa mediante Android Auto.
- Flujo: tarjetas cercanas → buscar dirección → tarjetas de la zona → detalle → navegar. Los resultados ofrecen volver a «Cerca de mí»; Atrás conserva la búsqueda.
- Cada tarjeta proporciona un marcador P en las coordenadas municipales de la plaza para el mapa de Android Auto.
- Las tarjetas cercanas al vehículo se reordenan automáticamente con ubicación vigente: al menos 50 m de desplazamiento y 10 s entre selecciones, con margen de 25 m para evitar oscilaciones. Mantienen títulos de posición estables y actualizan dirección, distancia y marcador conforme a las reglas de refresco del host. Las búsquedas por dirección conservan su origen fijo.
- Búsqueda nativa de direcciones en Android Auto, con cancelación de consultas anteriores y resultados ordenados desde la dirección resuelta; tarjetas de resultados y navegación externa hasta la plaza elegida. El detalle conserva el origen de la búsqueda al mostrar distancias.
- Distancia y dirección.
- Selección de destino y transferencia a una aplicación de navegación.

## Distribución inicial
- APK local para el teléfono.
- Desktop Head Unit durante el desarrollo.
- Internal App Sharing o pista interna de Google Play para probar Car App Library en un vehículo real.
