# Gravity Gun — Гравитационная пушка

Мод для **Minecraft 1.21 / 1.21.1** на **Fabric**: пушка, которая хватает мобов и блоки, держит их в воздухе перед тобой и швыряет с сокрушительной силой. Вдохновлено одной известной физической пушкой из Half-Life 2.

## Возможности

- **Захват мобов** — зомби, коровы, криперы, лодки, вагонетки, ТНТ и даже выпавшие предметы. Дальность луча — 16 блоков.
- **Захват блоков** — вырывай блок прямо из мира и таскай его с собой (земля, камень, руды, что угодно).
- **Мощный бросок** — запусти удерживаемую цель по направлению взгляда. Брошенные мобы получают урон при ударе о стены и сбивают других мобов, летящие блоки калечат всё на своём пути.
- **Аккуратная установка** — с шифтом цель мягко опускается на место: можно переносить блоки без разрушения и расставлять мобов как фигурки.
- Звуки, частицы и свечение удерживаемой цели прилагаются.

## Управление

| Действие | Результат |
|---|---|
| ПКМ по мобу или блоку | Захватить цель (до 16 блоков) |
| ПКМ, пока цель удерживается | Мощный бросок |
| Шифт + ПКМ | Аккуратно отпустить |

**Что нельзя захватить:** игроков, Дракона Края и Визера, бедрок и другие неразрушаемые блоки, а также блоки с инвентарём (сундуки, печи и т.п.) — ваши алмазы в безопасности.

## Крафт

```
Ж Ж Ж        Ж — железный слиток
Р О А        Р — блок редстоуна
Ж Ж Ж        О — око эндера,  А — алмаз
```

## Установка

1. Установи [Fabric Loader](https://fabricmc.net/use/) для Minecraft 1.21.1.
2. Скачай [Fabric API](https://modrinth.com/mod/fabric-api) и положи в папку `mods`.
3. Положи `gravity-gun-x.x.x.jar` в ту же папку `mods`.
4. Запусти игру и скрафти пушку (или возьми её из творческого раздела «Инструменты»).

## Сборка из исходников

Нужны **JDK 21** и **Gradle 8.12+**:

```bash
gradle build
# готовый мод: build/libs/gravity-gun-<версия>.jar
```

В репозитории нет ни одного бинарного файла: текстура и иконка мода хранятся как текстовые пиксель-карты в `texturegen/*.px` и растеризуются в PNG Gradle-задачей `generateTextures` прямо во время сборки. Перерисовать текстуру можно, отредактировав `texturegen/design.py` и запустив `python3 texturegen/design.py` (предпросмотр — `texturegen/preview.png`).

---

# Gravity Gun (English)

A **Fabric** mod for **Minecraft 1.21 / 1.21.1**: grab mobs and blocks, hold them mid-air and hurl them with devastating force. Inspired by a certain famous physics gun.

**Controls:** right-click to grab whatever you're looking at (16 block range), right-click again to launch it, sneak + right-click to set it down gently. Launched mobs take crash damage and bowl over anything living on their path; launched blocks hurt whatever they land on.

**Can't grab:** players, the Ender Dragon and the Wither, unbreakable blocks (bedrock) and blocks with inventories (chests, furnaces).

**Crafting (shaped, 3×3):** top and bottom rows — three iron ingots each; middle row — redstone block, eye of ender, diamond.

**Building:** JDK 21 + Gradle 8.12+, then `gradle build` → `build/libs/`. The repo is 100% text: textures are stored as `.px` pixel maps in `texturegen/` and rasterised to PNG by the `generateTextures` Gradle task at build time.

## License

MIT — see [LICENSE](LICENSE).
