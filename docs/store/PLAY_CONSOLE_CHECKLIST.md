# Checklist de Publicación en Google Play Console — Sudoku (Altikore)

Esta guía contiene todo lo que necesitas copiar y verificar para publicar **`com.altikore.sudoku`** en Google Play Console.

---

## 1. Obtener la URL pública (HTTPS) de la Política de Privacidad

El repositorio ya incluye una web estática lista en [`docs/index.html`](../index.html) (con selector en Español, Inglés y Portugués, sin rastreadores externos).

### Pasos recomendados (GitHub Pages — gratis):
1. Sube el proyecto a un repositorio público en GitHub (por ejemplo `altikore/sudoku`).
2. Ve a **Settings → Pages** en GitHub.
3. En **Build and deployment**:
   - **Source:** `Deploy from a branch`
   - **Branch:** `main` (o tu rama principal) y carpeta **`/docs`**
4. Guarda y espera ~1 minuto. Tu URL quedará así:
   - `https://<tu-usuario-github>.github.io/<tu-repositorio>/`
   *(Opcional: para usar tu dominio propio, configura el CNAME en GitHub Pages o sube los archivos de `docs/` a `altikore.com/privacy-policy`.)*

> ⚠️ **Importante:** Antes de subir, revisa que el correo `support@altikore.com` en [`docs/index.html`](../index.html) y en [`docs/store/privacy-policy.md`](privacy-policy.md) corresponda a tu buzón de soporte real.

---

## 2. Generar tu Keystore y compilar el App Bundle firmado (`.aab`)

Ejecuta en PowerShell desde la raíz del proyecto:

```powershell
.\scripts\build-play-release.ps1
```

Este script interactivo:
1. Si aún no existe `keystore.properties`, genera `altikore-sudoku-release.jks` (PKCS12, RSA 2048, alias `altikore-sudoku`) y configura `keystore.properties` sin guardar contraseñas en Git.
2. Compila con R8 (`isMinifyEnabled = true`, `isShrinkResources = true`):
   - `app/build/outputs/bundle/release/app-release.aab`
   - `app/build/outputs/apk/release/app-release.apk`
   - `app/build/outputs/mapping/release/mapping.txt`
3. Verifica la firma criptográfica con `jarsigner -verify`.

> 🔐 **Respaldo obligatorio:** Guarda `altikore-sudoku-release.jks` y tus contraseñas en un gestor de contraseñas o bóveda cifrada fuera del proyecto. Nunca subas el `.jks` ni `keystore.properties` a Git.

---

## 3. Crear la aplicación y completar «Contenido de la app»

En **Google Play Console → Todas las apps → Crear app**:
- **Nombre de la app:** `Sudoku`
- **Idioma predeterminado:** Español (España o Latinoamérica) — o Inglés
- **¿App o juego?:** **Juego**
- **¿Gratis o de pago?:** **Gratis**

Luego ve a **Política y programas → Contenido de la app** y completa cada sección:

| Sección | Qué responder / subir |
| --- | --- |
| **Política de privacidad** | URL HTTPS publicada en el paso 1 |
| **Acceso a la app** | **Todas las funciones están disponibles sin acceso restringido** (sin inicio de sesión) |
| **Anuncios** | **No, mi app no contiene anuncios** |
| **Clasificación de contenido (IARC)** | Categoría **Juego**; violencia = No; lenguaje/drogas/apuestas/interacción entre usuarios = **No**; compras dentro de la app = **No** → resultado esperado: **Todos / PEGI 3** |
| **Público objetivo** | Recomendado: **13-15, 16-17 y 18 o más** (si eliges menores de 13 años se activa la revisión adicional *Diseñado para familias*, aunque el Sudoku es apto para todo público) |
| **Seguridad de los datos (Data Safety)** | **¿Recopila o comparte datos de usuario?** → **No** (el manifest no declara `INTERNET` ni SDKs de analítica) |
| **ID de publicidad** | **No** utiliza ID de publicidad |
| **Apps gubernamentales / Noticias / Salud / Finanzas** | **No** en todos los formularios |

---

## 4. Configurar la ficha de Google Play Store

Ve a **Crecimiento → Presencia en Play Store → Ficha principal** (y añade traducciones en **Inglés** y **Portugués**):
- **Categoría:** **Juegos → Puzles**
- **Nombre, descripción corta y descripción larga:** copia los textos exactos de [`docs/store/play-store-listing.md`](play-store-listing.md)
- **Correo electrónico de contacto:** tu correo de soporte (ej. `support@altikore.com`)
- **Ícono de la app (`512 × 512 px`):** [`docs/store/play-store-icon-512.png`](play-store-icon-512.png)
- **Gráfico de funciones (`1024 × 500 px`):**
  - Español: [`docs/store/feature-graphic-1024x500-es.png`](feature-graphic-1024x500-es.png)
  - Inglés: [`docs/store/feature-graphic-1024x500-en.png`](feature-graphic-1024x500-en.png)
  - Portugués: [`docs/store/feature-graphic-1024x500-pt.png`](feature-graphic-1024x500-pt.png)
- **Capturas de pantalla:** sube las imágenes de [`docs/store/screenshots/`](screenshots/) (mínimo 2 de teléfono; aquí tienes galería completa de teléfono y tablet).

---

## 5. Subir el `.aab`, símbolos R8 y lanzar pruebas

1. Al crear la primera versión, acepta **Play App Signing** (obligatorio para apps nuevas; Google gestionará la clave final de Play Store y tu `.jks` será la *Upload key*).
2. Sube **`app/build/outputs/bundle/release/app-release.aab`**.
3. En **Símbolos de depuración / Desofuscación**, sube **`app/build/outputs/mapping/release/mapping.txt`** para poder leer stacktraces si alguna vez hubiera un fallo en producción.
4. Copia las notas de lanzamiento (`v1.0.0`) desde [`docs/store/play-store-listing.md`](play-store-listing.md).
5. **Ruta recomendada de lanzamiento:**
   - **Pruebas internas:** verifica que la descarga desde Google Play funcione y revisa el *Pre-launch report* automático de Google.
   - **Pruebas cerradas (si tu cuenta personal es nueva):** Google exige un mínimo de **12 testers inscritos de forma continua durante 14 días** antes de desbloquear el botón de solicitud a **Producción**. *(Las cuentas de organización con D-U-N-S están exentas de este paso.)*
   - **Producción:** envía a revisión (tarda normalmente entre 1 y 7 días).

---

## 6. Siguientes versiones (actualizaciones)

Para cada actualización futura en Google Play:
1. Incrementa `versionCode` (entero +1) y `versionName` en [`app/build.gradle.kts`](../../app/build.gradle.kts).
2. Ejecuta `.\scripts\build-play-release.ps1` usando **el mismo `altikore-sudoku-release.jks`**.
3. Sube el nuevo `app-release.aab` y `mapping.txt`.
