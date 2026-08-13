# Letters Hitboxes — Minecraft 1.20.6 / Fabric (client-side)

Клиентский мод, который **физически** увеличивает хитбоксы мобов и игроков — меняются реальные
`EntityDimensions` и, соответственно, реальный AABB (bounding box). Это не визуальный обман:
изменённый бокс используется для коллизий, движения, попаданий в ближнем бою, попаданий снарядов и
наведения прицела. Дополнительно можно **масштабировать модели** под новый бокс, чтобы моб выглядел
ровно так, как он бьётся.

* Minecraft: **1.20.6**
* Loader: **Fabric** (проверено на loader 0.19.3, работает с 0.15+)
* Требуется: **Fabric API**, **Java 21**
* `environment: "client"` — мод физически клиентский, на сервер его ставить не нужно.

Версия 1.1: профили с быстрым переключением, GUI настроек, визуальное масштабирование моделей.

---

## Как это работает (технически)

| Миксин | Цель | Что даёт |
|---|---|---|
| `LivingEntityDimensionsMixin` | `LivingEntity#getDimensions(Pose)` | все мобы и игроки — реальные размеры бокса |
| `EntityDimensionsMixin` | `Entity#getDimensions(Pose)` | не-живые сущности (лодки, вагонетки, стрелы, дропы) |
| `EntityPickRadiusMixin` | `Entity#getPickRadius()` | режим `TARGETING`: расширяется только бокс для рейкастов |
| `LivingEntityRendererMixin` | `LivingEntityRenderer#scale(...)` | визуальное масштабирование моделей мобов и игроков |
| `EntityRenderDispatcherMixin` | обёртка вызова `EntityRenderer#render` | визуальное масштабирование моделей не-живых сущностей (лодки, вагонетки, стрелы, дропы) |
| `EntityRendererShadowMixin` | `EntityRenderer#getShadowRadius` | тень по размеру увеличенной модели |

`LivingEntity#getDimensions` в 1.20.6 не вызывает `super`, поэтому двойного применения масштаба нет.
Из размеров пересобирается `EntityAttachments` (точки посадки/поводка), чтобы пассажиры не «висели в
воздухе». Масштаб модели вычисляется по той же формуле, что и физический бокс, и применяется **после**
ванильного `scale()`, так что детёныши и слизни сохраняют свою логику размера.

**Одиночная игра / LAN:** встроенный сервер работает в том же процессе, поэтому миксины действуют и на
серверной стороне. Хитбоксы реально физические: урон, коллизии и попадания считаются по новому боксу,
рассинхрона нет. Это основной режим работы мода.

**Удалённый сервер:** хитбоксы считает сервер, поэтому по умолчанию `applyOnMultiplayer: false` и мод
сам себя выключает. Это не способ «бить дальше» — сервер валидирует каждый удар по своим боксам;
включение даст только локальный рассинхрон. Включайте лишь там, где это разрешено (свой сервер, тесты).

---

## Установка

1. Fabric Loader для 1.20.6 + [Fabric API](https://modrinth.com/mod/fabric-api) (0.100.8+1.20.6).
2. Положите `letters-hitboxes-1.0.0.jar` в `.minecraft/mods/`.
3. Запустите игру — создастся `.minecraft/config/lettershitboxes.json`.

---

## GUI настроек

**F7** (или `/hitbox gui`) открывает экран настроек:

* мод вкл/выкл, переключение профиля одной кнопкой, кнопка режима (`OFF / DIMENSIONS / TARGETING / BOTH`);
* слайдеры: ширина/высота мобов, ширина других игроков, ширина своего игрока, «+блоки» к прицелу,
  предел множителя;
* галочки: масштабировать модели, масштабировать свою модель, тень по размеру модели,
  контуры хитбоксов, ванильный бокс, защита от застревания, применять на серверах (с предупреждением);
* слайдеры визуального масштаба модели по ширине/высоте;
* кнопки: Перечитать / Сбросить / Пресеты / Готово.

Раскладка адаптивная: шаг строк считается от высоты окна, поэтому на любом GUI-масштабе виджеты не
наезжают на нижний ряд кнопок.

Сверху видно, активен ли мод прямо сейчас, снизу — список профилей. Любое изменение сразу пишется в
json и пересчитывает боксы у загруженных сущностей.

## Клавиши

| Действие | По умолчанию |
|---|---|
| Вкл/выкл мод | **F6** |
| Открыть настройки | **F7** |
| Следующий профиль | не назначено |
| Перезагрузить конфиг | не назначено |
| Контуры хитбоксов | не назначено |
| Вкл/выкл масштаб моделей | не назначено |

Переназначается в *Настройки → Управление → Letters Hitboxes*.

---

## Профили

Правила лежат внутри профилей, между ними можно переключаться мгновенно (кнопка в GUI, клавиша или
команда). Встроенные пресеты:

| Профиль | Что делает |
|---|---|
| `default` | сбалансированно: мобы шире (монстры x1.4), свой игрок не тронут |
| `huge` | x2.0 ширина / x1.5 высота, модели следуют за боксом — хорошо видно, что изменение физическое |
| `targeting` | физика ванильная, увеличен только рейкаст-бокс (+0.35 блока) |
| `off` | всё ванильное — удобно для сравнения |

```
/hitbox profile                     — список (активный помечен)
/hitbox profile use <name>
/hitbox profile next
/hitbox profile new <name>          — копия активного
/hitbox profile copy <from> <to>
/hitbox profile delete <name>
/hitbox profile presets             — восстановить встроенные
```

Старый конфиг (до профилей) подхватывается автоматически: правила из корня файла переносятся в профиль
`migrated`, он же становится активным.

---

## Команды

```
/hitbox                       — статус
/hitbox gui                   — открыть настройки
/hitbox on | off | toggle
/hitbox reload | refresh | reset
/hitbox maxscale <1..64>
/hitbox multiplayer <bool>

/hitbox default <width> [height]      — правило по умолчанию (в активном профиле)
/hitbox default mode <OFF|DIMENSIONS|TARGETING|BOTH>
/hitbox default margin <блоки>        — прибавка к рейкаст-боксу
/hitbox default eye <множитель>
/hitbox self ...                      — свой игрок (те же под-команды)
/hitbox players ...                   — другие игроки
/hitbox category <monster|creature|...> <width> [height] | mode <...>
/hitbox type minecraft:creeper <width> [height] | mode <...> | margin <блоки> | remove

/hitbox visual models <bool>          — масштабировать модели
/hitbox visual self <bool>            — масштабировать свою модель
/hitbox visual shadow <bool>          — тень по размеру модели
/hitbox visual width <множитель>      — доп. множитель ширины модели
/hitbox visual height <множитель>

/hitbox blacklist add|remove <id> | clear
/hitbox boxes <bool> | boxes vanilla <bool> | boxes targeting <bool>
```

---

## Конфиг `config/lettershitboxes.json`

```json
{
  "enabled": true,
  "applyOnMultiplayer": false,
  "maxScale": 8.0,
  "protectSelfFromSuffocation": true,
  "autoRefreshEntities": true,
  "activeProfile": "default",
  "profiles": {
    "default": {
      "description": "Balanced: wider mobs, own player untouched",
      "defaults":     { "mode": "DIMENSIONS", "width": 1.25, "height": 1.0 },
      "selfPlayer":   { "mode": "OFF" },
      "otherPlayers": { "mode": "DIMENSIONS", "width": 1.25, "height": 1.0 },
      "byCategory": {
        "monster": { "mode": "DIMENSIONS", "width": 1.4, "height": 1.0 },
        "misc":    { "mode": "OFF" }
      },
      "byEntityId": {
        "minecraft:creeper": { "width": 1.6 },
        "minecraft:ghast":   { "mode": "TARGETING", "pickRadiusBonus": 0.5 }
      },
      "blacklist": ["minecraft:ender_dragon", "minecraft:falling_block", "minecraft:tnt", "minecraft:end_crystal"],
      "visual": {
        "scaleModels": true,
        "scaleSelfModel": true,
        "scaleShadow": true,
        "modelWidthFactor": 1.0,
        "modelHeightFactor": 1.0,
        "maxModelScale": 6.0
      }
    }
  },
  "debug": { "renderBoxes": false, "renderVanillaBox": true, "renderTargetingBox": false, "maxRenderDistance": 48.0 }
}
```

Приоритет внутри профиля: `blacklist` → `byEntityId` → `byCategory` → `selfPlayer`/`otherPlayers` → `defaults`.
Любое поле правила можно **не указывать** — оно наследуется от родительского (`{"width": 2.0}` валидно).

### Поля правила

| Поле | Описание |
|---|---|
| `mode` | `OFF` / `DIMENSIONS` (физический бокс) / `TARGETING` (только рейкаст) / `BOTH` |
| `width`, `height` | множители ширины (X/Z) и высоты (Y), `1.0` = ваниль |
| `eyeHeight` | доп. множитель высоты глаз |
| `widthBonus`, `heightBonus` | плоская прибавка в блоках (можно отрицательную) |
| `pickRadiusBonus` | прибавка к рейкаст-боксу в блоках |
| `scaleFixedDimensions` | масштабировать «fixed» размеры (шалкеры, рамки), которые ваниль не масштабирует |

### Визуальные поля (`visual`)

| Поле | Описание |
|---|---|
| `scaleModels` | масштабировать модели под физический бокс |
| `scaleSelfModel` | масштабировать свою модель (третье лицо / F5) |
| `scaleShadow` | масштабировать тень под размер модели |
| `modelWidthFactor`, `modelHeightFactor` | доп. множитель поверх соотношения бокса (`1.0` — точно по боксу) |
| `maxModelScale` | предохранитель масштаба модели |

### Глобальные поля

`enabled`, `applyOnMultiplayer`, `maxScale`, `protectSelfFromSuffocation`, `autoRefreshEntities`,
`activeProfile`, `debug.renderBoxes`, `debug.renderVanillaBox`, `debug.renderTargetingBox`,
`debug.maxRenderDistance`, `debug.logRuleResolution`.
В `blacklist` работает `*` в конце: `"minecraft:experience_*"`.

---

## Ограничения и подводные камни

* **Ники над головой** подстраиваются автоматически: `renderNameTag` берёт точку из
  `EntityAttachment.NAME_TAG`, а мы масштабируем `EntityAttachments` вместе с боксом.
* **Высота > 1.0 = риск задохнуться.** Раздутая по Y сущность может застрять в блоках; для своего
  игрока это ограничено флагом `protectSelfFromSuffocation`.
* **Масштаб моделей** покрывает и живые сущности, и остальные (лодки, вагонетки, стрелы, дропы).
  Ванильный контур F3+B и тень при этом считаются отдельно, без двойного масштабирования.
* Мод использует **MixinExtras** (`@WrapOperation`) — она входит в Fabric Loader 0.15+, ставить
  отдельно ничего не нужно.
* **Ender Dragon / End Crystal** в чёрном списке: у дракона бокс собирают отдельные части тела.
* **`fixed` размеры** (шалкер, рамка, стойка для брони) ваниль намеренно не масштабирует —
  включайте `scaleFixedDimensions: true` осознанно.
* **ИИ мобов** считает пути по своему боксу: очень широкие мобы хуже проходят в узкие щели.
* На удалённом сервере `TARGETING` не даёт реального «реча»: сервер всё равно проверяет дистанцию и
  свои хитбоксы.

---

## Сборка из исходников

```bash
JAVA_HOME=/path/to/jdk-21 ./gradlew build
# результат: build/libs/letters-hitboxes-1.0.0.jar
```

Mojang mappings (как в актуальном шаблоне Fabric), Gradle 9.5.1, Loom 1.17.
Код целиком в `src/client/java` (client sourceset), `fabric.mod.json` в `src/main/resources`.

Лицензия: MIT.
