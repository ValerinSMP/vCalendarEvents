# vCalendarEvents - Documentacion Tecnica

vCalendarEvents es un sistema avanzado de calendario y programacion de eventos para servidores de Minecraft, disenado para automatizar eventos del servidor con webhooks de Discord, GUIs interactivas y soporte completo de PlaceholderAPI.

---

## Requisitos y Dependencias

Para el correcto funcionamiento del plugin, asegurese de contar con las siguientes dependencias:

- **Java**: 21 o superior.
- **Servidor**: Paper/Purpur 1.21+.
- **PlaceholderAPI** (Opcional): Para el uso de placeholders %vevents_*% en otros plugins.

---

## Compilacion

Este proyecto utiliza Maven como sistema de construccion.

### Instrucciones de Compilacion

Clone el repositorio en su entorno local:

```bash
git clone https://github.com/tu-usuario/vCalendarEvents.git
```

Navegue al directorio del proyecto y ejecute el comando de construccion:

**Windows:**
```bash
mvn clean package
```

**Linux/macOS:**
```bash
mvn clean package
```

El archivo compilado (vCalendarEvents-1.0-SNAPSHOT-shaded.jar) se generara en la carpeta target/.

---

## Caracteristicas

### Sistema de Eventos

- **Programacion Flexible**: Horarios diarios o especificos por dia de la semana.
- **Acciones Multiples**: Comandos de consola, mensajes broadcast, titulos, sonidos y webhooks.
- **Sistema de Countdowns**: Avisos configurables (30m, 15m, 1m, etc.) con acciones especificas.
- **Instancias Cronologicas**: Eventos repetitivos aparecen multiples veces en el GUI si son los proximos en ocurrir.

### Webhooks de Discord

- **Integracion Nativa**: Sin plugins adicionales requeridos.
- **Embeds Personalizables**: Titulo, descripcion, color y footer configurables.
- **Rol Mentions**: Soporte para menciones de roles y contenido personalizado.
- **Identidad del Webhook**: Respeta el nombre y avatar configurados en Discord (opcional configurar manualmente).

### GUIs Interactivas

- **Menu Principal**: Muestra los proximos eventos en orden cronologico con countdowns individuales.
- **Calendario Mensual**: Vista de mes completo con dias de eventos destacados.
- **Seguridad Anti-Robo**: Previene arrastrar items y clics ilegales.
- **Completamente Personalizable**: Via gui.yml (titulos, items, slots, iconos, lore).

### Base de Datos SQLite

- **Persistencia Local**: Sin necesidad de MySQL.
- **Registro de Ejecuciones**: Para auditoria y seguimiento.
- **Optimizada**: Consultas rapidas sin lag.

### Mensajes y Formato

- **MiniMessage**: Formato de texto rico con colores hex, gradientes, hover/click events.
- **Completamente Traducible**: Via messages.yml.
- **Prefijo Configurable**: Con colores personalizados.
- **Titulos y ActionBars**: Para notificaciones visuales.

---

## Comandos y Permisos

### Comandos Principales

Alias disponibles: /vevents, /calendar, /eventos, /events, /pevents, /calendario

| Comando | Descripcion | Permiso |
|---------|-------------|---------|
| /eventos | Abre el menu principal de eventos | Sin permiso requerido |
| /eventos reload | Recarga todas las configuraciones | vcalendarevents.admin |
| /eventos list | Lista todos los eventos configurados | vcalendarevents.admin |

### Permisos

| Permiso | Descripcion | Por defecto |
|---------|-------------|-------------|
| vcalendarevents.admin | Acceso completo a comandos administrativos | OP |

---

## Placeholders (PlaceholderAPI)

El plugin exporta los siguientes placeholders para su uso en mensajes, scoreboards o menus:

**Identificador**: vevents

### Placeholders Disponibles

| Placeholder | Retorno (Ejemplo) | Descripcion |
|-------------|-------------------|-------------|
| %vevents_next_event_name% | Key All | Nombre del proximo evento |
| %vevents_next_event_time% | 19:30 | Hora del proximo evento |
| %vevents_countdown_<event_id>% | 02:15:30 | Countdown hasta un evento especifico |

**Ejemplo de uso con event IDs**:
- %vevents_countdown_keyall% - Tiempo hasta el evento Key All
- %vevents_countdown_koth_nokeep% - Tiempo hasta KOTH sin KeepInventory
- %vevents_countdown_megawarden% - Tiempo hasta Mega Warden Boss

---

## Configuracion

La configuracion se divide en multiples archivos para facilitar su gestion:

| Archivo | Descripcion |
|---------|-------------|
| config.yml | Configuracion principal de eventos, horarios, countdowns y acciones |
| messages.yml | Mensajes, prefijo y textos del plugin |
| gui.yml | Configuracion de menus (titulos, slots, items, lore, formato) |
| webhooks.yml | Webhooks de Discord con embeds personalizados |
| events.db | Base de datos SQLite (generada automaticamente) |

---

### Configuracion de Eventos (config.yml)

Cada evento se define con la siguiente estructura:

```yaml
events:
  keyall:
    display-name: "<color:#FFD180>KEY ALL"
    description:
      - "<gray>Todos los jugadores"
      - "<gray>recibiran llaves."
    icon: CYAN_CANDLE
    schedule:
      - "19:30"  # Diario a las 19:30
    countdown:
      - 900  # 15 minutos
      - 60   # 1 minuto
    actions:
      start:
        - "[console] crate key giveall vote"
        - "[message] %prefix%<green>Se han entregado llaves a todos"
        - "[title] <color:#FFD180>KEY ALL;<white>Disfruta tus premios"
        - "[sound] UI_TOAST_CHALLENGE_COMPLETE"
      countdown_900:
        - "[message] %prefix%<yellow>El Key All inicia en 15 minutos"
        - "[webhook] keyall"
      countdown:
        - "[message] %prefix%<yellow>El evento inicia en %time% segundos"
```

#### Tipos de Acciones

| Tipo | Sintaxis | Descripcion |
|------|----------|-------------|
| [console] | [console] comando | Ejecuta un comando desde consola |
| [message] | [message] texto | Envia un mensaje broadcast |
| [title] | [title] titulo;subtitulo | Muestra un titulo a todos los jugadores |
| [sound] | [sound] SOUND_NAME | Reproduce un sonido a todos |
| [webhook] | [webhook] nombre_webhook | Envia un webhook de Discord |

#### Placeholders en Acciones

- %prefix% - Prefijo del plugin (de messages.yml)
- %time% - Tiempo restante en segundos (para countdowns)
- %event_displayname% - Nombre del evento

---

### Configuracion de Webhooks (webhooks.yml)

```yaml
webhooks:
  keyall:
    url: "https://discord.com/api/webhooks/..."
    content: "@Eventos"  # Mencion de rol o texto
    embed:
      title: "Un evento esta a punto de iniciar"
      description:
        - "Evento: Key All"
        - "Modalidad: Survival"
        - "Inicio en: 15 minutos"
      color: "#FFD700"
      footer: "vCalendarEvents"
```

**Nota**: Los campos username y avatar son opcionales. Si no se especifican, se usa la configuracion del webhook en Discord.

---

### Sistema de Countdowns

El sistema de countdowns funciona con prioridad especifica:

1. **Countdown especifico** (countdown_900, countdown_1800): Se ejecuta cuando el tiempo coincide exactamente.
2. **Countdown generico** (countdown): Se usa como fallback para otros tiempos no especificos.

**Ejemplo**:

```yaml
countdown:
  - 1800  # 30 minutos
  - 900   # 15 minutos
  - 60    # 1 minuto

actions:
  countdown_1800:
    - "[webhook] evento_30m"
  countdown_900:
    - "[webhook] evento_15m"
  countdown:
    - "[message] %prefix%Inicia en %time% segundos"
```

En este caso:
- A los 30m: ejecuta countdown_1800
- A los 15m: ejecuta countdown_900
- A los 60s: ejecuta countdown (generico con %time%)

---

### Horarios Avanzados

#### Eventos Diarios

```yaml
schedule:
  - "19:30"  # Todos los dias a las 19:30
```

#### Eventos por Dia de la Semana

```yaml
schedule:
  - "MONDAY;22:00"
  - "TUESDAY;22:00"
  - "SATURDAY;20:30"
```

Dias soportados: MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY

---

### Configuracion del GUI (gui.yml)

```yaml
main_menu:
  title: "<gradient:#FFD180:#FF8A80>CALENDARIO DE EVENTOS"
  size: 54
  event_slots: [10, 11, 12, 13, 14, 15, 16]
  
  formatting:
    daily_event_time: "<gray>Hora: <color:#FFD180>{time}"
    daily_event_countdown: "<gray>Empieza en: <color:#FFD180>{duration}"
```

---

### Logs Detallados

Al iniciar o recargar el plugin, se muestra un resumen completo:

```
[vCalendarEvents] ------------------------------------------
[vCalendarEvents] Configuraciones actualizadas con exito
[vCalendarEvents]  >> Eventos del calendario: 6
[vCalendarEvents]  >> Webhooks operativos: 10
[vCalendarEvents]  >> Mensajes traducidos: 15
[vCalendarEvents]  >> Menus registrados: 3
[vCalendarEvents] ------------------------------------------
```

---

## Recarga en Caliente

Para aplicar cambios sin reiniciar el servidor:

```
/eventos reload
```

Esto recarga:
- config.yml (eventos, horarios, acciones)
- messages.yml (mensajes y textos)
- gui.yml (menus y formato)
- webhooks.yml (webhooks de Discord)
- Tareas programadas (re-calcula proximos eventos)

---

## Debug

Para activar logs detallados:

```yaml
# config.yml
settings:
  debug: true
```

Con debug activo se muestra:
- Numero de instancias de eventos generadas
- Proximas 5 instancias cronologicas con timestamps
- Detalles de slots y asignacion en GUIs
- Estado de ejecucion de webhooks

---

## Ejemplos de Configuracion

### Evento KOTH con Avisos

```yaml
events:
  koth_nokeep:
    display-name: "<color:#FFD180>KOTH (Sin KeepInventory)"
    icon: OMINOUS_TRIAL_KEY
    schedule:
      - "SATURDAY;20:30"
      - "SUNDAY;20:30"
    countdown:
      - 1800  # 30m
      - 900   # 15m
      - 60    # 1m
    actions:
      start:
        - "[webhook] koth_start"
      countdown_1800:
        - "[message] %prefix%<yellow>El KOTH inicia en 30 minutos"
        - "[webhook] koth_30m"
      countdown_900:
        - "[message] %prefix%<yellow>El KOTH inicia en 15 minutos"
        - "[webhook] koth_15m"
      countdown:
        - "[message] %prefix%<yellow>El KOTH inicia en %time% segundos"
```

### Evento con Toggle de GameRule

```yaml
events:
  minapvp_safe:
    display-name: "<green>MINAPVP SEGURA"
    icon: MEDIUM_AMETHYST_BUD
    schedule:
      - "14:00"
    actions:
      start:
        - "[message] %prefix%<green>La MinaPvP ahora es segura"
        - "[webhook] minapvp_enable"
        - "[console] mv gamerule set keepInventory true world_minapvp"
```

---

## Personalizacion Visual

### Colores MiniMessage

El plugin usa MiniMessage para formato de texto:

```yaml
# Colores hex
"<color:#FFD180>Texto"

# Gradientes
"<gradient:#FFD180:#FF8A80>Texto con gradiente"

# Estilos
"<bold>Negrita"
"<italic>Cursiva"

# Combinados
"<bold><gradient:#FFD180:#FF8A80>Texto negrita con gradiente"
```

### Iconos de Eventos

Se puede usar cualquier material de Minecraft 1.21:

```yaml
icon: CYAN_CANDLE        # Vela cian
icon: OMINOUS_TRIAL_KEY  # Llave ominosa
icon: WARDEN_SPAWN_EGG   # Huevo de Warden
icon: AMETHYST_CLUSTER   # Cluster de amatista
```

---

## Mejores Practicas

### Produccion

1. Desactive debug: settings.debug: false en produccion.
2. Zona horaria: Configure settings.time-zone segun su ubicacion.
3. Backups: Respalde events.db regularmente.

### Webhooks

1. Identidad en Discord: Configure nombre y avatar del webhook directamente en Discord.
2. Roles mentions: Use <@&ROLE_ID> en el campo content para mencionar roles.
3. Pruebas: Use /eventos reload para probar webhooks sin esperar al horario.

### Performance

1. Limite de instancias: El sistema genera 30 instancias por evento (suficiente para menus grandes).
2. Recarga inteligente: /eventos reload solo recarga configuraciones, no reinicia la base de datos.
3. GUI optimizado: Los countdowns se calculan individualmente sin lag.

---

## Licencia

Este plugin fue desarrollado por Antigravity. Todos los derechos reservados.
