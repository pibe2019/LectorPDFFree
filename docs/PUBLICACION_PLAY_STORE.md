# Publicación en Google Play — MyPDFFree

Checklist para pasar de este repositorio a la app publicada. Cuenta **personal anterior a noviembre de 2023**:
**no** aplica la prueba cerrada obligatoria de 12 testers durante 14 días; se puede ir directo a producción.

---

## 1. Clave de subida (una sola vez; **guárdala**: si la pierdes no puedes actualizar la app)

```bash
keytool -genkeypair -v -keystore mypdffree-upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
```

Crea `keystore.properties` en la **raíz del proyecto**. Ya está en `.gitignore`: nunca lo subas al repositorio.

```properties
storeFile=mypdffree-upload.jks
storePassword=TU_PASSWORD
keyAlias=upload
keyPassword=TU_PASSWORD
```

`app/build.gradle.kts` lo detecta solo. Sin este archivo, el release se firma con la clave de debug, y Play lo rechaza.
Guarda una copia del `.jks` y de las contraseñas fuera del ordenador (gestor de contraseñas o nube privada).

## 2. Generar el AAB

```bash
./gradlew bundleRelease
```

Sale en `app/build/outputs/bundle/release/app-release.aab`. Lleva dentro el mapping de R8, así que Play desofusca los crashes sin que tengas que subir nada aparte.
**En cada versión nueva:** sube `versionCode` (1, 2, 3…) y `versionName` en `app/build.gradle.kts`.

## 3. Play Console → Crear app

- Nombre: **MyPDFFree** · Idioma predeterminado: Español · Tipo: App · Gratis
- **Firma de apps de Play**: aceptar (Google guarda la clave de firma final; la tuya es solo la de subida)

## 4. Contenido de la app (Política → Contenido de la app)

| Sección | Respuesta |
|---|---|
| Política de privacidad | URL pública de `docs/POLITICA_DE_PRIVACIDAD.md` (GitHub Pages, Google Sites o un Gist público) |
| Anuncios | No contiene anuncios |
| Acceso a la app | Todas las funciones están disponibles sin acceso especial |
| Clasificación de contenido | Cuestionario: categoría «Utilidad/Productividad»; todo «No» → PEGI 3 / Para todos |
| Público objetivo | 13+ (o 18+). Evita marcar menores de 13: activa requisitos extra de la política de familias |
| Seguridad de los datos | «¿Recopila o comparte datos?» → **No**. El historial no sale del dispositivo, así que no cuenta como recopilación |
| App de noticias / gubernamental / salud / financiera | No |

## 5. Ficha de Play Store

| Recurso | Archivo / texto |
|---|---|
| Icono 512×512 | `docs/play-store/icon-512.png` |
| Gráfico destacado 1024×500 | `docs/play-store/feature-graphic-1024x500.png` |
| Capturas de teléfono (mín. 2) | Hacerlas en el móvil: Home con recientes, lector, zoom, «Ir a página» |
| Descripción breve (≤80) | `Lee tus PDF gratis: rápido, sin anuncios y sin permisos de almacenamiento.` |
| Categoría | Productividad |

Descripción completa sugerida:

```
MyPDFFree es un lector de PDF gratuito, ligero y respetuoso con tu privacidad.

• Abre cualquier PDF de tu teléfono o desde otras apps con «Abrir con».
• Zoom con dos dedos o doble toque, y desplazamiento continuo entre páginas.
• Retoma cada documento en la página donde lo dejaste.
• Salta a cualquier página al instante.
• Tema claro y oscuro, con los colores de tu sistema (Material You).
• Sin anuncios, sin cuentas, sin conexión a internet y sin permisos de almacenamiento.

Tus documentos nunca salen de tu teléfono.
```

## 6. Publicar

Producción → Crear versión → subir el `.aab` → notas de versión («Primera versión») → Revisar → **Enviar a revisión**.
La primera revisión suele tardar entre unas horas y 7 días.

## Antes de subir: prueba en un móvil real

- [ ] Abrir un PDF desde «Abrir PDF» y desde un gestor de archivos («Abrir con»)
- [ ] Zoom con pellizco y doble toque; desplazamiento horizontal con zoom
- [ ] Rotar la pantalla en el lector: se conserva la página
- [ ] Cerrar y reabrir: el documento aparece en recientes y retoma la página
- [ ] Un PDF con contraseña muestra el aviso, sin cerrarse la app
- [ ] Tema oscuro
- [ ] Probar el **AAB de release** (no solo debug): R8 puede romper cosas que en debug funcionan
