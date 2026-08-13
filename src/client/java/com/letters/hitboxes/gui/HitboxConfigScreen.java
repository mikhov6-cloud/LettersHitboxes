package com.letters.hitboxes.gui;

import com.letters.hitboxes.HitboxEngine;
import com.letters.hitboxes.config.ConfigManager;
import com.letters.hitboxes.config.HitboxConfig;
import com.letters.hitboxes.config.HitboxMode;
import com.letters.hitboxes.config.HitboxProfile;
import com.letters.hitboxes.config.ScaleRule;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** In-game settings screen (F7 by default, or /hitbox gui). */
public class HitboxConfigScreen extends Screen {

    private static final int COL_W = 150;
    private static final int GAP = 4;
    private static final int ROWS = 9;

    private final Screen parent;

    private int top;
    private int step;
    private int widgetH;
    private int leftX;
    private int rightX;
    private int leftRow;
    private int rightRow;

    public HitboxConfigScreen(Screen parent) {
        super(Component.literal("Letters Hitboxes"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        HitboxConfig cfg = ConfigManager.get();
        HitboxProfile profile = cfg.active();
        ScaleRule defaults = profile.defaults;
        ScaleRule others = profile.otherPlayers;
        ScaleRule self = profile.selfPlayer;
        float maxSlider = Math.min(cfg.maxScale, 6.0F);

        // adaptive layout so nothing overlaps the bottom row on short screens
        top = 40;
        int bottomY = this.height - 34;
        int available = Math.max(72, bottomY - top);
        step = Math.min(24, available / ROWS);
        widgetH = Math.max(12, step - 2);
        leftX = this.width / 2 - COL_W - GAP;
        rightX = this.width / 2 + GAP;
        leftRow = 0;
        rightRow = 0;

        // ---------------------------------------------------------- left column
        addRenderableWidget(Button.builder(
                        Component.literal("Мод: " + onOff(cfg.enabled)),
                        button -> {
                            cfg.enabled = !cfg.enabled;
                            ConfigManager.applyChanges();
                            button.setMessage(Component.literal("Мод: " + onOff(cfg.enabled)));
                        })
                .bounds(leftX, nextLeft(), COL_W, widgetH)
                .tooltip(Tooltip.create(Component.literal("Главный выключатель (клавиша F6)")))
                .build());

        addRenderableWidget(Button.builder(
                        Component.literal("Профиль: " + cfg.activeProfile),
                        button -> {
                            ConfigManager.nextProfile();
                            rebuild();
                        })
                .bounds(leftX, nextLeft(), COL_W, widgetH)
                .tooltip(Tooltip.create(Component.literal("Переключить профиль. Доступно: "
                        + String.join(", ", ConfigManager.profileNames())
                        + (profile.description == null || profile.description.isBlank()
                        ? "" : "\n" + profile.description))))
                .build());

        addRenderableWidget(Button.builder(
                        Component.literal("Режим: " + mode(defaults)),
                        button -> {
                            defaults.mode = nextMode(mode(defaults));
                            ConfigManager.applyChanges();
                            button.setMessage(Component.literal("Режим: " + defaults.mode));
                        })
                .bounds(leftX, nextLeft(), COL_W, widgetH)
                .tooltip(Tooltip.create(Component.literal(
                        "DIMENSIONS — реальный физический бокс\n"
                                + "TARGETING — только бокс для прицела/снарядов\n"
                                + "BOTH — и то и другое\nOFF — выключено")))
                .build());

        addRenderableWidget(new FloatSlider(leftX, nextLeft(), COL_W, widgetH, "Мобы: ширина", "x",
                0.25F, maxSlider, value(defaults.width), value -> {
            defaults.width = value;
            enableDimensions(defaults);
            ConfigManager.applyChanges();
        }));

        addRenderableWidget(new FloatSlider(leftX, nextLeft(), COL_W, widgetH, "Мобы: высота", "x",
                0.25F, maxSlider, value(defaults.height), value -> {
            defaults.height = value;
            enableDimensions(defaults);
            ConfigManager.applyChanges();
        }));

        addRenderableWidget(new FloatSlider(leftX, nextLeft(), COL_W, widgetH, "Игроки: ширина", "x",
                0.25F, maxSlider, value(others.width), value -> {
            others.width = value;
            enableDimensions(others);
            ConfigManager.applyChanges();
        }));

        addRenderableWidget(new FloatSlider(leftX, nextLeft(), COL_W, widgetH, "Свой игрок: ширина", "x",
                0.25F, maxSlider, value(self.width), value -> {
            self.width = value;
            enableDimensions(self);
            ConfigManager.applyChanges();
        }));

        addRenderableWidget(new FloatSlider(leftX, nextLeft(), COL_W, widgetH, "Прицел: +блоки", "",
                0.0F, 2.0F, value(defaults.pickRadiusBonus, 0.0F), value -> {
            defaults.pickRadiusBonus = value;
            if (value > 0.0F && mode(defaults) == HitboxMode.DIMENSIONS) defaults.mode = HitboxMode.BOTH;
            ConfigManager.applyChanges();
        }));

        addRenderableWidget(new FloatSlider(leftX, nextLeft(), COL_W, widgetH, "Предел множителя", "x",
                1.0F, 16.0F, cfg.maxScale, value -> {
            cfg.maxScale = value;
            ConfigManager.applyChanges();
        }));

        // ---------------------------------------------------------- right column
        addRenderableWidget(Checkbox.builder(Component.literal("Масштабировать модели"), this.font)
                .pos(rightX, nextRight())
                .selected(profile.visual.scaleModels)
                .onValueChange((checkbox, value) -> {
                    profile.visual.scaleModels = value;
                    ConfigManager.applyChanges();
                })
                .tooltip(Tooltip.create(Component.literal(
                        "Модель визуально совпадает с физическим боксом\n(мобы, игроки, а также лодки/дропы/стрелы)")))
                .build());

        addRenderableWidget(Checkbox.builder(Component.literal("Своя модель тоже"), this.font)
                .pos(rightX, nextRight())
                .selected(profile.visual.scaleSelfModel)
                .onValueChange((checkbox, value) -> {
                    profile.visual.scaleSelfModel = value;
                    ConfigManager.applyChanges();
                })
                .build());

        addRenderableWidget(Checkbox.builder(Component.literal("Тень по размеру модели"), this.font)
                .pos(rightX, nextRight())
                .selected(profile.visual.scaleShadow)
                .onValueChange((checkbox, value) -> {
                    profile.visual.scaleShadow = value;
                    ConfigManager.applyChanges();
                })
                .build());

        addRenderableWidget(new FloatSlider(rightX, nextRight(), COL_W, widgetH, "Модель: ширина", "x",
                0.25F, 3.0F, profile.visual.modelWidthFactor, value -> {
            profile.visual.modelWidthFactor = value;
            ConfigManager.applyChanges();
        }));

        addRenderableWidget(new FloatSlider(rightX, nextRight(), COL_W, widgetH, "Модель: высота", "x",
                0.25F, 3.0F, profile.visual.modelHeightFactor, value -> {
            profile.visual.modelHeightFactor = value;
            ConfigManager.applyChanges();
        }));

        addRenderableWidget(Checkbox.builder(Component.literal("Контуры хитбоксов"), this.font)
                .pos(rightX, nextRight())
                .selected(cfg.debug.renderBoxes)
                .onValueChange((checkbox, value) -> {
                    cfg.debug.renderBoxes = value;
                    ConfigManager.applyChanges();
                })
                .build());

        addRenderableWidget(Checkbox.builder(Component.literal("+ ванильный бокс"), this.font)
                .pos(rightX, nextRight())
                .selected(cfg.debug.renderVanillaBox)
                .onValueChange((checkbox, value) -> {
                    cfg.debug.renderVanillaBox = value;
                    ConfigManager.applyChanges();
                })
                .build());

        addRenderableWidget(Checkbox.builder(Component.literal("Не раздувать себя по высоте"), this.font)
                .pos(rightX, nextRight())
                .selected(cfg.protectSelfFromSuffocation)
                .onValueChange((checkbox, value) -> {
                    cfg.protectSelfFromSuffocation = value;
                    ConfigManager.applyChanges();
                })
                .tooltip(Tooltip.create(Component.literal("Защита от застревания в 2-блочных проходах")))
                .build());

        addRenderableWidget(Checkbox.builder(Component.literal("Применять на серверах"), this.font)
                .pos(rightX, nextRight())
                .selected(cfg.applyOnMultiplayer)
                .onValueChange((checkbox, value) -> {
                    cfg.applyOnMultiplayer = value;
                    ConfigManager.applyChanges();
                })
                .tooltip(Tooltip.create(Component.literal(
                        "На удалённом сервере хитбоксы считает сервер.\n"
                                + "Изменения будут только локальными → возможен рассинхрон,\n"
                                + "и это не даёт бить дальше: сервер проверяет каждый удар.\n"
                                + "Включайте только на своём сервере / с разрешения.")))
                .build());

        // ---------------------------------------------------------- bottom row
        int bottom = this.height - 26;
        int smallW = 84;
        int startX = this.width / 2 - (smallW * 2 + GAP + GAP / 2);
        addRenderableWidget(Button.builder(Component.literal("Перечитать"), button -> {
            ConfigManager.load();
            rebuild();
        }).bounds(startX, bottom, smallW, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Сбросить"), button -> {
            ConfigManager.reset();
            rebuild();
        }).bounds(startX + smallW + GAP, bottom, smallW, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Пресеты"), button -> {
            ConfigManager.restorePresets();
            rebuild();
        }).bounds(startX + (smallW + GAP) * 2, bottom, smallW, 20)
                .tooltip(Tooltip.create(Component.literal(
                        "Вернуть встроенные профили: default, huge, targeting, off")))
                .build());

        addRenderableWidget(Button.builder(Component.literal("Готово"), button -> onClose())
                .bounds(startX + (smallW + GAP) * 3, bottom, smallW, 20).build());
    }

    private int nextLeft() {
        return top + step * leftRow++;
    }

    private int nextRight() {
        return top + step * rightRow++;
    }

    private void rebuild() {
        clearWidgets();
        init();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        HitboxConfig cfg = ConfigManager.get();
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);

        String state = HitboxEngine.globallyActive()
                ? "§aактивно сейчас"
                : (cfg.enabled ? "§eне применяется здесь (см. «Применять на серверах»)" : "§cвыключено");
        graphics.drawCenteredString(this.font, Component.literal(
                "§7профиль §f" + cfg.activeProfile + "§7 — " + state), this.width / 2, 24, 0xAAAAAA);
    }

    @Override
    public void onClose() {
        ConfigManager.applyChanges();
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }

    private static void enableDimensions(ScaleRule rule) {
        if (rule.mode == null || rule.mode == HitboxMode.OFF) {
            rule.mode = HitboxMode.DIMENSIONS;
        }
    }

    private static String onOff(boolean value) {
        return value ? "§aвкл" : "§cвыкл";
    }

    private static float value(Float raw) {
        return value(raw, 1.0F);
    }

    private static float value(Float raw, float fallback) {
        return raw != null ? raw : fallback;
    }

    private static HitboxMode mode(ScaleRule rule) {
        return rule.mode != null ? rule.mode : HitboxMode.DIMENSIONS;
    }

    private static HitboxMode nextMode(HitboxMode mode) {
        HitboxMode[] values = HitboxMode.values();
        return values[(mode.ordinal() + 1) % values.length];
    }
}
