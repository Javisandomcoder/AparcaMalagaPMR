# AparcaMálaga PMR

Aplicación Android nativa para localizar plazas de aparcamiento reservadas para personas con movilidad reducida en Málaga y abrir una aplicación de navegación hasta ellas.

## Estado funcional

- Aplicación móvil en Kotlin, Jetpack Compose y Material 3.
- Listado de 1.355 ubicaciones municipales válidas.
- Búsqueda por dirección o descripción, sin distinguir mayúsculas ni tildes.
- Número de plazas y observaciones municipales cuando están disponibles.
- **Ubicación opcional:** el botón **Ordenar por cercanía** solicita permiso sólo si se pulsa.
- **Cálculo local de distancia** y ordenación de la más cercana a la más lejana.
- La última ubicación se recuerda con la fecha de captura y sólo se reutiliza durante cinco minutos, con permiso y ubicación del dispositivo activados.
- Las posiciones antiguas, sin fecha o con coordenadas inválidas se descartan; sin posición reciente se ocultan las distancias.
- **Room** conserva las 1.355 plazas como última copia válida.
- Primera carga desde la copia GeoJSON incluida; después se consulta Room.
- Comprobación automática como máximo una vez cada 24 horas.
- Botón **Actualizar datos** para forzar una comprobación manual.
- Reemplazo transaccional: una descarga sólo sustituye la caché tras parsearse por completo.
- Si la red o el recurso fallan, se conserva la última copia y la aplicación sigue operativa.
- Acción **Navegar** mediante URI `geo:` para Google Maps, Waze u otra aplicación compatible.
- Aviso explícito: la ubicación no implica disponibilidad en tiempo real.
- Android Auto mediante Car App Library 1.7.0 y categoría `POI`. La pantalla principal abre tarjetas cercanas en `PlaceListMapTemplate`, con el mapa del host para situar sus marcadores.
- **Buscar dirección** lleva a la búsqueda nativa; al enviarla aparecen tarjetas próximas a esa dirección. **Cerca de mí** en los resultados vuelve al inicio y Atrás conserva la consulta.
- Se retira del flujo la pantalla propia de seguimiento, cámara animada y cartografía OpenStreetMap. El guiado corresponde a la aplicación externa de navegación elegida por Android Auto (Google Maps en la configuración del usuario).
- La selección abre un detalle estable con distancia en línea recta, atribución y navegación externa.
- La pantalla del coche carga la caché de forma asíncrona, muestra un indicador de carga y permite reintentar si falla la lectura.
- En el coche, seleccionar una plaza abre su detalle con atribución, aviso de disponibilidad y acción **Navegar**.
- Las tarjetas del coche comprueban la vigencia de ubicación cada 5 segundos; el teléfono, cada 30 segundos. La suscripción a ubicación se detiene al ocultar la lista. Sin una ubicación vigente, las tarjetas no muestran distancias desde el vehículo.
- El listado del coche actualiza automáticamente las plazas por cercanía tras desplazarse 50 m, con un mínimo de 10 s entre selecciones y un margen de 25 m para evitar intercambios constantes. Conserva títulos numerados por posición; dirección, distancia, marcador y destino corresponden siempre a la plaza mostrada. Sin ubicación vigente conserva las tarjetas y oculta las distancias. El refresco manual vuelve a ordenar inmediatamente en Car API 5+. Las búsquedas por dirección mantienen su origen fijo.
- Misma capa de dominio y datos para teléfono y coche.

## Fuente oficial y copia local

- Conjunto: [Aparcamientos movilidad reducida — Ayuntamiento de Málaga](https://datosabiertos.malaga.eu/dataset/aparcamientos-movilidad-reducida)
- Recurso: [GeoJSON EPSG:4326](https://datosabiertos.malaga.eu/recursos/transporte/trafico/da_aparcamientosMovilidadReducida-4326.geojson)
- Licencia publicada: CC BY-SA 4.0.
- Última modificación indicada por el portal para el recurso: 12/05/2025.
- Copia incluida en el APK descargada el 30/08/2026.
- Registros recibidos: 1.356; registros utilizados: 1.355.

El parser descarta el registro `ID 66296` porque el recurso oficial devuelve una latitud inválida (`-4.4524995`) fuera de Málaga. No se corrige ni se inventa una coordenada alternativa.

La copia local está en:

```text
app/src/main/assets/aparcamientos_pmr_malaga_4326.geojson
```

## Configuración

- Paquete: `com.javisandom.aparcamalagapmr`
- Versión actual: `0.4.0` (`versionCode 4`)
- `minSdk 26`, `targetSdk 37`, `compileSdk 37`
- Android Gradle Plugin 9.3.2
- Gradle 9.7.1
- Kotlin 2.4.10
- Car App Library 1.7.0
- Room 2.8.4

## Principios

- Uso personal inicial.
- Sin cuentas, servidor, analítica ni publicidad.
- Una ubicación conocida no implica disponibilidad en tiempo real.
- La navegación giro a giro se delega a una aplicación instalada.
- No se muestran ubicaciones ficticias como datos municipales.

## Verificación

La búsqueda del teléfono prepara un índice normalizado del catálogo en segundo plano. El campo de texto se actualiza al escribir y filtra tras 200 ms de pausa; las consultas anteriores se cancelan. Si no hay coincidencias, espera otros 450 ms antes de resolver la dirección con el servicio Geocoder del dispositivo, limitado a seis segundos. El proveedor puede recibir el texto de la dirección consultada; no se exige permiso de ubicación para buscar direcciones. Los resultados válidos se conservan en una caché de sesión de 32 direcciones. Se muestran hasta 30 plazas municipales ordenadas respecto a la dirección resuelta, con su nombre y origen de distancias visibles. Si no se puede resolver, se indica que hay que revisar dirección/conexión; no se inventa un destino.

«Ordenar por cercanía» limpia la búsqueda por dirección, muestra una posición reciente válida si existe y solicita red/GPS en paralelo durante un máximo de ocho segundos. Utiliza la primera medición válida y cancela la otra solicitud. Las solicitudes se cancelan al salir de la pantalla. La precisión y rapidez reales siguen dependiendo de la señal y del servicio del dispositivo.

Validación de 0.4.0: 40 pruebas unitarias y 20 instrumentadas en emulador, `assembleDebug`, `assembleDebugAndroidTest` y `lintDebug` correctos. Prueba manual de geocodificación con «Calle Larios 1»: el servicio devolvió «C. Marqués de Larios, 1» y la lista comenzó por CALLE CORTINA DEL MUELLE, 1, a 256 m en línea recta. Esta versión todavía requiere comprobar la sensación de escritura y el tiempo de obtención de ubicación en el teléfono físico; las carreras entre proveedores, caducidad y cancelación se comprobaron con pruebas controladas.

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug assembleDebugAndroidTest
./gradlew connectedDebugAndroidTest
./gradlew lintDebug
```

La validación automática no sustituye la prueba de Android Auto mediante Desktop Head Unit y posteriormente en un vehículo real.

## Cartografía del coche

Mapa: © [OpenStreetMap contributors](https://www.openstreetmap.org/copyright). Se solicitan únicamente las teselas visibles, con identificador de la aplicación, tres descargas simultáneas como máximo, caché RAM de 80 teselas y caché en disco durante al menos siete días. No se descargan ciudades ni recorridos por adelantado. El proveedor recibe las solicitudes de las zonas de mapa consultadas; las plazas PMR siguen procediendo exclusivamente del Ayuntamiento.

Sin red se utiliza la cartografía ya consultada; donde no haya teselas se indica «Cartografía no disponible» y se mantienen los marcadores municipales. No se garantiza un mapa completo sin conexión. El dibujo tiene modo diurno/nocturno y adapta la posición del vehículo y los marcadores al área no tapada por las plantillas del host.

Prueba del 6 de septiembre de 2026 con Xiaomi Redmi Note 12 Pro 5G, Android Auto 17.5.663214 y DHU 2.0: comprobados mapa con cartografía y ubicación real, pulsación de marcador y ficha correspondiente, zoom, modo diurno/nocturno, listado secundario y detalle, regreso al mapa, botón Centrar y apertura de navegación en Google Maps. Se canceló la ruta de prueba al terminar. No aparecieron excepciones de AndroidRuntime en el tramo revisado.

Al actualizar desde la versión anterior se observaron controles sobre fondo negro y ausencia de callbacks de superficie. Reiniciar por completo el proceso de Android Auto permitió recibir una superficie válida y dibujar el mapa; una simple reconexión previa no lo había resuelto. Se mantuvo además MapWithContentTemplate durante la carga, con prueba de regresión, aunque ese cambio por sí solo no resolvió el fondo negro. Hubo interrupciones de transporte USB durante la sesión, sin causa determinada. Las verificaciones finales pasaron: 31 pruebas unitarias, 18 instrumentadas en emulador, ambas compilaciones y lint sin errores.

Pendiente: desplazamiento manual prolongado, seguimiento durante un trayecto real y renovación de candidatos cada 75 m, conservación de la selección durante ese movimiento, comprobación sin permisos/red y sesiones largas sin agotar pasos del host. Pulsar Centrar con el mapa ya centrado no valida por sí solo el retorno desde una exploración manual. Las pruebas en DHU con el teléfono estacionario no sustituyen la validación en vehículo.

### Android Auto 0.5.0

- Buscar una dirección de Málaga desde el mapa o el listado. El sistema del coche controla la entrada de texto/voz.
- Los resultados indican distancias en línea recta desde la dirección; «Ver mapa» muestra sus marcadores. «Navegar» en el detalle abre navegación externa.
- El mapa principal sigue al vehículo con orientación deducida de desplazamientos de al menos 15 m. Al explorar conserva la orientación; «Seguir vehículo» recupera el seguimiento. No calcula rutas ni muestra instrucciones de giro propias.
- Pendiente de validación de búsqueda por voz y orientación en un vehículo real.

### Android Auto 0.5.2

- Tarjetas numeradas que cambian de aparcamiento automáticamente según la cercanía al vehículo. La dirección sigue visible en la primera línea y el marcador y la acción de detalle se actualizan con ella.
- Reordenación limitada a una vez cada 10 segundos tras moverse 50 m, con 25 m de margen entre candidatos para evitar oscilaciones. La primera ubicación válida y el refresco explícito ordenan inmediatamente.
- Suscripción a ubicación sólo mientras la lista está en primer plano. Los resultados de búsqueda por dirección conservan su referencia fija.
- Verificación: 55 pruebas unitarias, 28 instrumentadas en emulador, `assembleDebug`, `assembleDebugAndroidTest`, `lintDebug` y bundle de publicación firmado correctos. Pendiente comprobar esta versión y el seguimiento de 0.5.1 en vehículo real.

### Android Auto 0.5.1

Seguimiento más estable: confirmación de giros grandes, filtrado de pequeñas oscilaciones, transiciones de al menos 1,4 s y giro de cámara limitado a 30 grados/s. Las ubicaciones repetidas no reinician la animación. El indicador del vehículo permanece fijo durante el seguimiento y la exploración conserva la orientación. Pendiente de prueba real.

### Android Auto 0.6.0

Flujo centrado en tarjetas y búsqueda tras la prueba del usuario en vehículo real: el seguimiento propio resultó poco preciso y poco fluido. Inicio directo en tarjetas en todos los hosts; «Buscar dirección», «Ver tarjetas» y «Cerca de mí» integran ambas consultas. El detalle conserva la distancia desde la dirección buscada. Se elimina el permiso de superficie propia; el mapa del host sigue situando las plazas. No se calculan ni dibujan rutas en la aplicación.

Validación: 55 pruebas unitarias y 31 instrumentadas en emulador correctas; `assembleDebug`, `assembleDebugAndroidTest`, `lintDebug` y bundle de publicación firmado correctos. Pendiente comprobar el flujo simplificado en vehículo real.
