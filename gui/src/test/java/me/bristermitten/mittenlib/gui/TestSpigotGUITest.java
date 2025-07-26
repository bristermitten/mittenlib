package me.bristermitten.mittenlib.gui;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import me.bristermitten.mittenlib.gui.spigot.SpigotInventoryViewer;
import org.bukkit.inventory.InventoryView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;


public class TestSpigotGUITest {

    private ServerMock server;

    @BeforeEach
    void setup() {
        server = MockBukkit.mock();
    }

    @Test
    @Timeout(value = 30)
    public void test() {
        PlayerMock player = server.addPlayer();
        TestSpigotGUI testSpigotGUI = new TestSpigotGUI();
        new GUIExecutor<>(testSpigotGUI)
                .execute(new SpigotInventoryViewer<>(player));

        assertThat(player.getOpenInventory())
                .isNotNull()
                .extracting(InventoryView::getTitle)
                .isEqualTo("Counter GUI");
    }
}
