# AparcaMálaga PMR — instrucciones del proyecto

Antes de modificar el proyecto, lee `PRODUCT_SPEC.md` y trata sus requisitos como vinculantes.

- No uses ubicaciones ficticias como datos reales.
- Mantén separadas la UI Compose del teléfono y las plantillas de Android Auto.
- Comparte dominio y datos entre ambas superficies.
- Conserva la categoría Android Auto `POI`.
- Implementa cambios de comportamiento mediante TDD: prueba RED, implementación GREEN y refactorización.
- Verifica como mínimo `testDebugUnitTest`, `assembleDebug`, `assembleDebugAndroidTest` y `lintDebug`.
- La validación automática no sustituye la prueba en un vehículo real con Android Auto.
