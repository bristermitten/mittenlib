package me.bristermitten.mittenlib.gui.spigot;

import me.bristermitten.mittenlib.collections.MLImmutableMap;
import me.bristermitten.mittenlib.collections.Maps;
import me.bristermitten.mittenlib.gui.view.View;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Map;
import java.util.Optional;

/**
 * A view of a Spigot GUI - not a real inventory, but a description of one.
 *
 * @param <Msg> the message type
 */
public class SpigotGUIView<Msg> implements View<Msg, SpigotGUIView<Msg>, SpigotInventoryViewer<Msg>> {
    private final int size;
    private final String title;

    private final @Unmodifiable MLImmutableMap<Integer, InventoryButton<Msg>> buttons;
    private final @Nullable Msg onClose;

    public SpigotGUIView(int size, String title, @Unmodifiable Map<Integer, InventoryButton<Msg>> buttons, @Nullable Msg onClose) {
        this.size = size;
        this.title = title;
        this.buttons = Maps.of(buttons.entrySet());
        this.onClose = onClose;
    }

    public static <Msg> SpigotGUIView<Msg> create(int size, String title) {
        return new SpigotGUIView<>(size, title, Maps.of(), null);
    }

    public SpigotGUIView<Msg> withButton(int slot, InventoryButton<Msg> button) {
        return new SpigotGUIView<>(size, title, buttons.plus(slot, button), null);
    }

    public SpigotGUIView<Msg> onClose(@Nullable Msg onClose) {
        return new SpigotGUIView<>(size, title, buttons, onClose);
    }

    public Optional<InventoryButton<Msg>> getButton(int slot) {
        return Optional.ofNullable(buttons.get(slot));
    }

    public MLImmutableMap<Integer, InventoryButton<Msg>> getButtons() {
        return buttons;
    }

    public String getTitle() {
        return title;
    }

    public int getSize() {
        return size;
    }

    public @Nullable Msg getOnClose() {
        return onClose;
    }
}
