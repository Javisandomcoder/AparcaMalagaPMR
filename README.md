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
- Android Auto mediante Car App Library 1.7.0 y categoría `POI`. En Car API 7+ la pantalla principal utiliza `MapWithContentTemplate` con cartografía OpenStreetMap y marcadores municipales; en hosts anteriores conserva `PlaceListMapTemplate`.
- Seguimiento del vehículo con solicitudes de ubicación cada 2 segundos / 5 metros mientras el mapa está visible. Las posiciones nuevas desplazan la cámara suavemente; se renuevan hasta 24 candidatos cada 75 metros y se omiten marcadores superpuestos.
- Exploración, zoom y **Centrar** mediante controles del host. La interacción directa con marcadores depende del soporte táctil del host y puede requerir activar su modo de exploración. **Listado** ofrece una alternativa para seleccionar plazas.
- La selección abre un detalle estable con distancia en línea recta, atribución y navegación externa.
- La pantalla del coche carga la caché de forma asíncrona, muestra un indicador de carga y permite reintentar si falla la lectura.
- En el coche, seleccionar una plaza abre su detalle con atribución, aviso de disponibilidad y acción **Navegar**.
- El mapa comprueba la vigencia cada 5 segundos; el teléfono y el listado del coche, cada 30 segundos. El seguimiento se detiene al ocultar el mapa. Tras 20 segundos sin una medición reciente indica «Última ubicación disponible»; tras cinco minutos elimina la posición del vehículo.
- El listado secundario mantiene sus filas estables. Su refresco manual vuelve a seleccionar las plazas próximas en Car API 5+.
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
- Versión actual: `0.3.0` (`versionCode 3`)
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
