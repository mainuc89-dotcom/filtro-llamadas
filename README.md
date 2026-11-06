# Filtro

Bloqueo e identificación de llamadas para Android. Sin anuncios, sin cuentas y sin internet: todo queda en tu teléfono.

## Qué hace

- **Bloquea por prefijo**: viene con `800` activado (y `809`, el prefijo del telemarketing no solicitado en Chile). Puedes agregar cualquier otro, incluso países (`+234`).
- **Muestra quién llama** en una tarjeta sobre la pantalla de llamada: celular, teléfono fijo con su ciudad, empresa con servicio contratado (`600`), línea `800`, organismo público (`702`), telemarketing (`809`), telefonía IP (`44`) o llamada internacional con el país. Si un `+1` es del Caribe y no de EE.UU., lo advierte.
- **Tus contactos siempre suenan**: Android no se los pasa a Filtro, y Filtro no pide permiso para leer tu agenda.
- **Registro** de las llamadas revisadas, con acciones: copiar, buscar en internet, ponerle nombre, bloquear o permitir siempre.
- **Consulta rápida**: escribe un número en Inicio, o selecciónalo en cualquier app y elige *Consultar en Filtro*.
- Al bloquear puedes **rechazar** (no suena ni deja aviso) o **silenciar** (entra sin sonar y queda como perdida).

## Instalar

1. En el celular abre **https://github.com/mainuc89-dotcom/filtro-llamadas/releases/latest/download/Filtro.apk** (descarga directa de la última versión).
2. Abre el archivo descargado y acepta instalar. Si Android pide permiso para instalar apps desde el navegador, dalo.
   Si Play Protect avisa que no reconoce la app: *Más detalles → Instalar de todas formas*.
3. Abre Filtro → **Activar filtro** → elige *Filtro* como app de identificación de llamadas y spam.
4. Toca **Permitir** en *Ver quién llama* y activa *Mostrar sobre otras apps*.

Si Android dice "Configuración restringida": Ajustes → Apps → Filtro → menú ⋮ → *Permitir configuración restringida*, y vuelve a intentarlo.

## Notas

- Requiere Android 10 o superior.
- Solo una app puede filtrar llamadas a la vez. Al activar Filtro, la app anterior deja de hacerlo y puedes desinstalarla.
- Android no entrega los números ocultos a las apps de filtrado. Para bloquearlos usa el ajuste de tu app Teléfono.
- La llave de firma (`keystore/`) está en el repositorio para que cada versión nueva se instale encima de la anterior sin perder tus reglas.

## Cómo se compila

Cada cambio en `main` se compila y se prueba en un emulador Android 14 con GitHub Actions (`.github/workflows/android.yml`). El script `ci/emulator-test.sh` simula llamadas desde 800, 809, 600, celulares, fijos y un número internacional; revisa el bloqueo, el modo silenciar, el identificador con pantalla bloqueada y en modo oscuro. Si el mensaje del commit incluye `[capturas]`, las capturas y registros quedan en la rama `ci-resultados`. Al crear una etiqueta `v*` se publica el APK en *Releases*.

También puedes abrir la carpeta en Android Studio y compilar desde ahí.
