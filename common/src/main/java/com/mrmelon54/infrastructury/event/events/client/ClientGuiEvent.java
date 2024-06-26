package com.mrmelon54.infrastructury.event.events.client;

import com.mrmelon54.infrastructury.event.*;
import com.mrmelon54.infrastructury.hooks.client.screen.ScreenAccess;
import com.mrmelon54.infrastructury.utils.Graphics;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.util.List;
import java.util.function.Function;

@Environment(EnvType.CLIENT)
public interface ClientGuiEvent {
    interface Inner extends remapped.architectury.event.events.client.ClientGuiEvent {
    }

    static remapped.architectury.event.events.client.ClientGuiEvent.ScreenInitPre mapScreenInitPre(ScreenInitPre x) {
        #if MC_VER == MC_1_16_5
        return (screen, a, b) -> EventResult.map2(x.init(screen, () -> screen));
        #else
        return (screen, screenAccess) -> EventResult.map(x.init(screen, screenAccess::getScreen));
        #endif
    }

    static remapped.architectury.event.events.client.ClientGuiEvent.ScreenInitPost mapScreenInitPost(ScreenInitPost x) {
        #if MC_VER == MC_1_16_5
        return (screen, a, b) -> x.init(screen, () -> screen);
        #else
        return (screen, screenAccess) -> x.init(screen, screenAccess::getScreen);
        #endif
    }

    Event<RenderHud> RENDER_HUD = EventWrapper.of(Inner.RENDER_HUD, x -> (graphics, tickDelta) -> x.renderHud(Graphics.get(graphics), tickDelta));
    Event<DebugText> DEBUG_TEXT_LEFT = EventWrapper.of(Inner.DEBUG_TEXT_LEFT, x -> x::gatherText);
    Event<DebugText> DEBUG_TEXT_RIGHT = EventWrapper.of(Inner.DEBUG_TEXT_RIGHT, x -> x::gatherText);
    Event<ScreenInitPre> INIT_PRE = EventWrapper.of(Inner.INIT_PRE, ClientGuiEvent::mapScreenInitPre);
    Event<ScreenInitPost> INIT_POST = EventWrapper.of(Inner.INIT_POST, ClientGuiEvent::mapScreenInitPost);
    Event<ScreenRenderPre> RENDER_PRE = EventWrapper.of(Inner.RENDER_PRE, x -> (screen, graphics, mouseX, mouseY, delta) -> EventResult.map2(x.render(screen, Graphics.get(graphics), mouseX, mouseY, delta)));
    Event<ScreenRenderPost> RENDER_POST = EventWrapper.of(Inner.RENDER_POST, x -> (screen, poseStack, i, i1, v) -> x.render(screen, Graphics.get(poseStack), i, i1, v));
    PartialEvent<ContainerScreenRenderBackground> RENDER_CONTAINER_BACKGROUND = EventWrapper.partial(() -> {
        #if MC_VER > MC_1_17_1
        return EventWrapper.of(Inner.RENDER_CONTAINER_BACKGROUND, x -> (abstractContainerScreen, guiGraphics, i, i1, v) -> x.render(abstractContainerScreen, Graphics.get(guiGraphics), i, i1, v));
        #else
        return null;
        #endif
    });
    PartialEvent<ContainerScreenRenderForeground> RENDER_CONTAINER_FOREGROUND = EventWrapper.partial(() -> {
        #if MC_VER > MC_1_17_1
        return EventWrapper.of(Inner.RENDER_CONTAINER_FOREGROUND, x -> (screen, graphics, mouseX, mouseY, delta) -> x.render(screen, Graphics.get(graphics), mouseX, mouseY, delta));
        #else
        return null;
        #endif
    });
    Event<SetScreen> SET_SCREEN = EventWrapper.of(Inner.SET_SCREEN, x -> screen -> CompoundEventResult.map2(x.modifyScreen(screen)));

    @Environment(EnvType.CLIENT)
    interface RenderHud {
        void renderHud(GuiGraphics graphics, float tickDelta);
    }

    @Environment(EnvType.CLIENT)
    interface DebugText {
        void gatherText(List<String> strings);
    }

    @Environment(EnvType.CLIENT)
    interface ScreenInitPre {
        EventResult init(Screen screen, ScreenAccess access);
    }

    @Environment(EnvType.CLIENT)
    interface ScreenInitPost {
        void init(Screen screen, ScreenAccess access);
    }

    @Environment(EnvType.CLIENT)
    interface ScreenRenderPre {
        EventResult render(Screen screen, GuiGraphics graphics, int mouseX, int mouseY, float delta);
    }

    @Environment(EnvType.CLIENT)
    interface ScreenRenderPost {
        void render(Screen screen, GuiGraphics graphics, int mouseX, int mouseY, float delta);
    }

    @Environment(EnvType.CLIENT)
    interface ContainerScreenRenderBackground {
        void render(AbstractContainerScreen<?> screen, GuiGraphics graphics, int mouseX, int mouseY, float delta);
    }

    @Environment(EnvType.CLIENT)
    interface ContainerScreenRenderForeground {
        void render(AbstractContainerScreen<?> screen, GuiGraphics graphics, int mouseX, int mouseY, float delta);
    }

    @Environment(EnvType.CLIENT)
    interface SetScreen {
        CompoundEventResult<Screen> modifyScreen(Screen screen);
    }
}
