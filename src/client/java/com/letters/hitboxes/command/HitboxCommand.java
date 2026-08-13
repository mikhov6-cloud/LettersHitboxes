package com.letters.hitboxes.command;

import com.letters.hitboxes.HitboxEngine;
import com.letters.hitboxes.config.ConfigManager;
import com.letters.hitboxes.config.HitboxConfig;
import com.letters.hitboxes.config.HitboxMode;
import com.letters.hitboxes.config.HitboxProfile;
import com.letters.hitboxes.config.ScaleRule;
import com.letters.hitboxes.gui.HitboxConfigScreen;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.Map;

/** Client side {@code /hitbox ...} command. Nothing is sent to the server. */
public final class HitboxCommand {

    private HitboxCommand() {
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("hitbox")
                .executes(ctx -> status(ctx.getSource()))
                .then(ClientCommandManager.literal("status").executes(ctx -> status(ctx.getSource())))
                .then(ClientCommandManager.literal("gui").executes(ctx -> {
                    Minecraft mc = Minecraft.getInstance();
                    mc.execute(() -> mc.setScreen(new HitboxConfigScreen(null)));
                    return 1;
                }))
                .then(ClientCommandManager.literal("on").executes(ctx -> setEnabled(ctx.getSource(), true)))
                .then(ClientCommandManager.literal("off").executes(ctx -> setEnabled(ctx.getSource(), false)))
                .then(ClientCommandManager.literal("toggle")
                        .executes(ctx -> setEnabled(ctx.getSource(), !ConfigManager.get().enabled)))
                .then(ClientCommandManager.literal("reload").executes(ctx -> {
                    ConfigManager.load();
                    return feedback(ctx.getSource(), "§econfig reloaded from " + ConfigManager.path());
                }))
                .then(ClientCommandManager.literal("refresh").executes(ctx -> {
                    HitboxEngine.refreshLoadedEntities();
                    return feedback(ctx.getSource(), "§erebuilt hitboxes of all loaded entities");
                }))
                .then(ClientCommandManager.literal("multiplayer")
                        .then(ClientCommandManager.argument("value", BoolArgumentType.bool())
                                .executes(ctx -> {
                                    boolean value = BoolArgumentType.getBool(ctx, "value");
                                    ConfigManager.get().applyOnMultiplayer = value;
                                    ConfigManager.applyChanges();
                                    return feedback(ctx.getSource(), "applyOnMultiplayer = " + value
                                            + (value ? " §c(сервер всё равно считает удары по своим хитбоксам - будет рассинхрон; включайте только там, где это разрешено)" : ""));
                                })))
                .then(ClientCommandManager.literal("maxscale")
                        .then(ClientCommandManager.argument("value", FloatArgumentType.floatArg(1.0F, 64.0F))
                                .executes(ctx -> {
                                    ConfigManager.get().maxScale = FloatArgumentType.getFloat(ctx, "value");
                                    ConfigManager.applyChanges();
                                    return feedback(ctx.getSource(), "maxScale = " + ConfigManager.get().maxScale);
                                })))
                // ------------------------------------------------------------ profiles
                .then(ClientCommandManager.literal("profile")
                        .executes(ctx -> profileList(ctx.getSource()))
                        .then(ClientCommandManager.literal("list").executes(ctx -> profileList(ctx.getSource())))
                        .then(ClientCommandManager.literal("next").executes(ctx ->
                                feedback(ctx.getSource(), "профиль: §f" + ConfigManager.nextProfile())))
                        .then(ClientCommandManager.literal("use")
                                .then(ClientCommandManager.argument("name", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                ConfigManager.profileNames(), builder))
                                        .executes(ctx -> {
                                            String name = StringArgumentType.getString(ctx, "name");
                                            return ConfigManager.useProfile(name)
                                                    ? feedback(ctx.getSource(), "профиль: §f" + name)
                                                    : error(ctx.getSource(), "нет профиля " + name);
                                        })))
                        .then(ClientCommandManager.literal("new")
                                .then(ClientCommandManager.argument("name", StringArgumentType.word())
                                        .executes(ctx -> {
                                            String name = StringArgumentType.getString(ctx, "name");
                                            return ConfigManager.createProfile(name, null)
                                                    ? feedback(ctx.getSource(), "создан профиль §f" + name
                                                    + "§r (копия активного), переключитесь: /hitbox profile use " + name)
                                                    : error(ctx.getSource(), "профиль " + name + " уже есть или имя пустое");
                                        })))
                        .then(ClientCommandManager.literal("copy")
                                .then(ClientCommandManager.argument("from", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                ConfigManager.profileNames(), builder))
                                        .then(ClientCommandManager.argument("to", StringArgumentType.word())
                                                .executes(ctx -> {
                                                    String from = StringArgumentType.getString(ctx, "from");
                                                    String to = StringArgumentType.getString(ctx, "to");
                                                    return ConfigManager.createProfile(to, from)
                                                            ? feedback(ctx.getSource(), from + " -> " + to)
                                                            : error(ctx.getSource(), "не удалось скопировать");
                                                }))))
                        .then(ClientCommandManager.literal("delete")
                                .then(ClientCommandManager.argument("name", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                ConfigManager.profileNames(), builder))
                                        .executes(ctx -> {
                                            String name = StringArgumentType.getString(ctx, "name");
                                            return ConfigManager.deleteProfile(name)
                                                    ? feedback(ctx.getSource(), "профиль " + name + " удалён")
                                                    : error(ctx.getSource(), "нельзя удалить последний/неизвестный профиль");
                                        })))
                        .then(ClientCommandManager.literal("presets").executes(ctx -> {
                            ConfigManager.restorePresets();
                            return feedback(ctx.getSource(), "встроенные профили восстановлены: "
                                    + String.join(", ", HitboxProfile.presets().keySet()));
                        })))
                // ------------------------------------------------------------ visuals
                .then(ClientCommandManager.literal("visual")
                        .then(ClientCommandManager.literal("models")
                                .then(ClientCommandManager.argument("value", BoolArgumentType.bool())
                                        .executes(ctx -> {
                                            ConfigManager.profile().visual.scaleModels = BoolArgumentType.getBool(ctx, "value");
                                            ConfigManager.applyChanges();
                                            return feedback(ctx.getSource(), "visual.scaleModels = "
                                                    + ConfigManager.profile().visual.scaleModels);
                                        })))
                        .then(ClientCommandManager.literal("self")
                                .then(ClientCommandManager.argument("value", BoolArgumentType.bool())
                                        .executes(ctx -> {
                                            ConfigManager.profile().visual.scaleSelfModel = BoolArgumentType.getBool(ctx, "value");
                                            ConfigManager.applyChanges();
                                            return feedback(ctx.getSource(), "visual.scaleSelfModel = "
                                                    + ConfigManager.profile().visual.scaleSelfModel);
                                        })))
                        .then(ClientCommandManager.literal("shadow")
                                .then(ClientCommandManager.argument("value", BoolArgumentType.bool())
                                        .executes(ctx -> {
                                            ConfigManager.profile().visual.scaleShadow = BoolArgumentType.getBool(ctx, "value");
                                            ConfigManager.applyChanges();
                                            return feedback(ctx.getSource(), "visual.scaleShadow = "
                                                    + ConfigManager.profile().visual.scaleShadow);
                                        })))
                        .then(ClientCommandManager.literal("width")
                                .then(ClientCommandManager.argument("factor", FloatArgumentType.floatArg(0.05F, 8.0F))
                                        .executes(ctx -> {
                                            ConfigManager.profile().visual.modelWidthFactor = FloatArgumentType.getFloat(ctx, "factor");
                                            ConfigManager.applyChanges();
                                            return feedback(ctx.getSource(), "visual.modelWidthFactor = "
                                                    + ConfigManager.profile().visual.modelWidthFactor);
                                        })))
                        .then(ClientCommandManager.literal("height")
                                .then(ClientCommandManager.argument("factor", FloatArgumentType.floatArg(0.05F, 8.0F))
                                        .executes(ctx -> {
                                            ConfigManager.profile().visual.modelHeightFactor = FloatArgumentType.getFloat(ctx, "factor");
                                            ConfigManager.applyChanges();
                                            return feedback(ctx.getSource(), "visual.modelHeightFactor = "
                                                    + ConfigManager.profile().visual.modelHeightFactor);
                                        }))))
                // ------------------------------------------------------------ rules
                .then(ruleNode("default", ctx -> ConfigManager.profile().defaults, "defaults"))
                .then(ruleNode("self", ctx -> ConfigManager.profile().selfPlayer, "selfPlayer"))
                .then(ruleNode("players", ctx -> ConfigManager.profile().otherPlayers, "otherPlayers"))
                .then(ClientCommandManager.literal("category")
                        .then(ClientCommandManager.argument("name", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                        ConfigManager.profile().byCategory.keySet(), builder))
                                .then(ClientCommandManager.argument("width", FloatArgumentType.floatArg(0.05F, 64.0F))
                                        .executes(ctx -> setScale(ctx, categoryRule(ctx), "category "
                                                + StringArgumentType.getString(ctx, "name")))
                                        .then(ClientCommandManager.argument("height", FloatArgumentType.floatArg(0.05F, 64.0F))
                                                .executes(ctx -> setScale(ctx, categoryRule(ctx), "category "
                                                        + StringArgumentType.getString(ctx, "name")))))
                                .then(ClientCommandManager.literal("mode")
                                        .then(ClientCommandManager.argument("mode", StringArgumentType.word())
                                                .suggests(HitboxCommand::suggestModes)
                                                .executes(ctx -> setMode(ctx, categoryRule(ctx), "category "
                                                        + StringArgumentType.getString(ctx, "name")))))))
                .then(ClientCommandManager.literal("type")
                        .then(ClientCommandManager.argument("id", StringArgumentType.string())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                                        BuiltInRegistries.ENTITY_TYPE.keySet(), builder))
                                .then(ClientCommandManager.argument("width", FloatArgumentType.floatArg(0.05F, 64.0F))
                                        .executes(ctx -> setScale(ctx, typeRule(ctx), "type "
                                                + StringArgumentType.getString(ctx, "id")))
                                        .then(ClientCommandManager.argument("height", FloatArgumentType.floatArg(0.05F, 64.0F))
                                                .executes(ctx -> setScale(ctx, typeRule(ctx), "type "
                                                        + StringArgumentType.getString(ctx, "id")))))
                                .then(ClientCommandManager.literal("mode")
                                        .then(ClientCommandManager.argument("mode", StringArgumentType.word())
                                                .suggests(HitboxCommand::suggestModes)
                                                .executes(ctx -> setMode(ctx, typeRule(ctx), "type "
                                                        + StringArgumentType.getString(ctx, "id")))))
                                .then(ClientCommandManager.literal("margin")
                                        .then(ClientCommandManager.argument("blocks", FloatArgumentType.floatArg(0.0F, 8.0F))
                                                .executes(ctx -> setMargin(ctx, typeRule(ctx), "type "
                                                        + StringArgumentType.getString(ctx, "id")))))
                                .then(ClientCommandManager.literal("remove")
                                        .executes(ctx -> {
                                            String id = StringArgumentType.getString(ctx, "id");
                                            ConfigManager.profile().byEntityId.remove(id);
                                            ConfigManager.applyChanges();
                                            return feedback(ctx.getSource(), "removed override for " + id);
                                        }))))
                .then(ClientCommandManager.literal("blacklist")
                        .then(ClientCommandManager.literal("add")
                                .then(ClientCommandManager.argument("id", StringArgumentType.string())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                                                BuiltInRegistries.ENTITY_TYPE.keySet(), builder))
                                        .executes(ctx -> {
                                            String id = StringArgumentType.getString(ctx, "id");
                                            if (!ConfigManager.profile().blacklist.contains(id)) {
                                                ConfigManager.profile().blacklist.add(id);
                                            }
                                            ConfigManager.applyChanges();
                                            return feedback(ctx.getSource(), "blacklisted " + id);
                                        })))
                        .then(ClientCommandManager.literal("remove")
                                .then(ClientCommandManager.argument("id", StringArgumentType.string())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                ConfigManager.profile().blacklist, builder))
                                        .executes(ctx -> {
                                            String id = StringArgumentType.getString(ctx, "id");
                                            ConfigManager.profile().blacklist.remove(id);
                                            ConfigManager.applyChanges();
                                            return feedback(ctx.getSource(), "un-blacklisted " + id);
                                        })))
                        .then(ClientCommandManager.literal("clear").executes(ctx -> {
                            ConfigManager.profile().blacklist.clear();
                            ConfigManager.applyChanges();
                            return feedback(ctx.getSource(), "blacklist cleared");
                        })))
                .then(ClientCommandManager.literal("boxes")
                        .then(ClientCommandManager.argument("value", BoolArgumentType.bool())
                                .executes(ctx -> {
                                    ConfigManager.get().debug.renderBoxes = BoolArgumentType.getBool(ctx, "value");
                                    ConfigManager.applyChanges();
                                    return feedback(ctx.getSource(), "debug.renderBoxes = "
                                            + ConfigManager.get().debug.renderBoxes);
                                }))
                        .then(ClientCommandManager.literal("vanilla")
                                .then(ClientCommandManager.argument("value", BoolArgumentType.bool())
                                        .executes(ctx -> {
                                            ConfigManager.get().debug.renderVanillaBox = BoolArgumentType.getBool(ctx, "value");
                                            ConfigManager.applyChanges();
                                            return feedback(ctx.getSource(), "debug.renderVanillaBox = "
                                                    + ConfigManager.get().debug.renderVanillaBox);
                                        })))
                        .then(ClientCommandManager.literal("targeting")
                                .then(ClientCommandManager.argument("value", BoolArgumentType.bool())
                                        .executes(ctx -> {
                                            ConfigManager.get().debug.renderTargetingBox = BoolArgumentType.getBool(ctx, "value");
                                            ConfigManager.applyChanges();
                                            return feedback(ctx.getSource(), "debug.renderTargetingBox = "
                                                    + ConfigManager.get().debug.renderTargetingBox);
                                        }))))
                .then(ClientCommandManager.literal("reset").executes(ctx -> {
                    ConfigManager.reset();
                    return feedback(ctx.getSource(), "§econfig reset to defaults");
                })));
    }

    private interface RuleSupplier {
        ScaleRule get(CommandContext<FabricClientCommandSource> ctx);
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> ruleNode(
            String literal, RuleSupplier supplier, String label) {
        return ClientCommandManager.literal(literal)
                .then(ClientCommandManager.argument("width", FloatArgumentType.floatArg(0.05F, 64.0F))
                        .executes(ctx -> setScale(ctx, supplier.get(ctx), label))
                        .then(ClientCommandManager.argument("height", FloatArgumentType.floatArg(0.05F, 64.0F))
                                .executes(ctx -> setScale(ctx, supplier.get(ctx), label))))
                .then(ClientCommandManager.literal("mode")
                        .then(ClientCommandManager.argument("mode", StringArgumentType.word())
                                .suggests(HitboxCommand::suggestModes)
                                .executes(ctx -> setMode(ctx, supplier.get(ctx), label))))
                .then(ClientCommandManager.literal("margin")
                        .then(ClientCommandManager.argument("blocks", FloatArgumentType.floatArg(0.0F, 8.0F))
                                .executes(ctx -> setMargin(ctx, supplier.get(ctx), label))))
                .then(ClientCommandManager.literal("eye")
                        .then(ClientCommandManager.argument("scale", FloatArgumentType.floatArg(0.05F, 8.0F))
                                .executes(ctx -> {
                                    ScaleRule rule = supplier.get(ctx);
                                    rule.eyeHeight = FloatArgumentType.getFloat(ctx, "scale");
                                    ConfigManager.applyChanges();
                                    return feedback(ctx.getSource(), label + ": eyeHeight = " + rule.eyeHeight);
                                })));
    }

    private static ScaleRule categoryRule(CommandContext<FabricClientCommandSource> ctx) {
        String name = StringArgumentType.getString(ctx, "name");
        Map<String, ScaleRule> map = ConfigManager.profile().byCategory;
        return map.computeIfAbsent(name, key -> new ScaleRule());
    }

    private static ScaleRule typeRule(CommandContext<FabricClientCommandSource> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        Map<String, ScaleRule> map = ConfigManager.profile().byEntityId;
        return map.computeIfAbsent(id, key -> new ScaleRule());
    }

    private static int setScale(CommandContext<FabricClientCommandSource> ctx, ScaleRule rule, String label) {
        float width = FloatArgumentType.getFloat(ctx, "width");
        float height;
        try {
            height = FloatArgumentType.getFloat(ctx, "height");
        } catch (IllegalArgumentException ignored) {
            height = rule.height != null ? rule.height : width;
        }
        rule.width = width;
        rule.height = height;
        if (rule.mode == null || rule.mode == HitboxMode.OFF) {
            rule.mode = HitboxMode.DIMENSIONS;
        }
        ConfigManager.applyChanges();
        return feedback(ctx.getSource(), label + ": width x" + width + ", height x" + height
                + " (mode " + rule.mode + ", профиль " + ConfigManager.get().activeProfile + ")");
    }

    private static int setMode(CommandContext<FabricClientCommandSource> ctx, ScaleRule rule, String label) {
        String raw = StringArgumentType.getString(ctx, "mode").toUpperCase(java.util.Locale.ROOT);
        HitboxMode mode;
        try {
            mode = HitboxMode.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return error(ctx.getSource(), "неизвестный режим " + raw + " - доступно "
                    + Arrays.toString(HitboxMode.values()));
        }
        rule.mode = mode;
        ConfigManager.applyChanges();
        return feedback(ctx.getSource(), label + ": mode = " + mode);
    }

    private static int setMargin(CommandContext<FabricClientCommandSource> ctx, ScaleRule rule, String label) {
        rule.pickRadiusBonus = FloatArgumentType.getFloat(ctx, "blocks");
        if (rule.mode == null || rule.mode == HitboxMode.OFF) {
            rule.mode = HitboxMode.BOTH;
        }
        ConfigManager.applyChanges();
        return feedback(ctx.getSource(), label + ": pickRadiusBonus = " + rule.pickRadiusBonus
                + " блоков (mode " + rule.mode + ")");
    }

    private static int setEnabled(FabricClientCommandSource source, boolean value) {
        ConfigManager.get().enabled = value;
        ConfigManager.applyChanges();
        return feedback(source, "Letters Hitboxes " + (value ? "§aвключён" : "§cвыключен"));
    }

    private static int profileList(FabricClientCommandSource source) {
        HitboxConfig cfg = ConfigManager.get();
        StringBuilder sb = new StringBuilder("§6[Hitbox] профили:§r");
        cfg.profiles.forEach((name, profile) -> sb.append("\n ")
                .append(name.equals(cfg.activeProfile) ? "§a> " : "  ")
                .append("§f").append(name).append("§7")
                .append(profile.description == null || profile.description.isBlank()
                        ? "" : " — " + profile.description));
        source.sendFeedback(Component.literal(sb.toString()));
        return 1;
    }

    private static int status(FabricClientCommandSource source) {
        HitboxConfig cfg = ConfigManager.get();
        HitboxProfile profile = cfg.active();
        source.sendFeedback(Component.literal("§6[Letters Hitboxes]§r"
                + "\n enabled: " + cfg.enabled + " | активно сейчас: " + HitboxEngine.globallyActive()
                + "\n профиль: §f" + cfg.activeProfile + "§r (" + String.join(", ", ConfigManager.profileNames()) + ")"
                + "\n applyOnMultiplayer: " + cfg.applyOnMultiplayer + " | maxScale: " + cfg.maxScale
                + "\n defaults: " + profile.defaults
                + "\n self: " + profile.selfPlayer
                + "\n other players: " + profile.otherPlayers
                + "\n категорий: " + profile.byCategory.size() + " | типов: " + profile.byEntityId.size()
                + " | blacklist: " + profile.blacklist.size()
                + "\n модели: " + profile.visual.scaleModels + " (x" + profile.visual.modelWidthFactor
                + " / x" + profile.visual.modelHeightFactor + ") | контуры: " + cfg.debug.renderBoxes
                + "\n конфиг: " + ConfigManager.path()));
        return 1;
    }

    private static int feedback(FabricClientCommandSource source, String message) {
        source.sendFeedback(Component.literal("§6[Hitbox]§r " + message));
        return 1;
    }

    private static int error(FabricClientCommandSource source, String message) {
        source.sendError(Component.literal(message));
        return 0;
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestModes(
            CommandContext<FabricClientCommandSource> ctx, com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(
                Arrays.stream(HitboxMode.values()).map(Enum::name).toList(), builder);
    }
}
