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
- Pantalla principal con mapa propio en `MapWithContentTemplate` (Car API 7+), seguimiento del vehículo y marcadores próximos que se renuevan durante el desplazamiento.
- Detalle al seleccionar una plaza y listado secundario; `PlaceListMapTemplate` como alternativa para sistemas anteriores.
- Distancia y dirección.
- Selección de destino y transferencia a una aplicación de navegación.

## Distribución inicial
- APK local para el teléfono.
- Desktop Head Unit durante el desarrollo.
- Internal App Sharing o pista interna de Google Play para probar Car App Library en un vehículo real.
