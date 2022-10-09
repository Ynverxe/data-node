import com.github.ynverxe.data.DataNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

public class DataNodeTest {

    @Test
    public void testNonSerializableTypeAdd() {
        DataNode dataNode = new DataNode();

        assertThrows(IllegalArgumentException.class, () -> dataNode.put("value", new StringBuilder()));
        assertThrows(IllegalArgumentException.class, () -> dataNode.put("value", Collections.singletonList(new StringBuilder())));
        assertThrows(IllegalArgumentException.class, () -> dataNode.put("node", Collections.singletonMap("value", new StringBuilder())));
    }

    @Test
    public void testMalFormattedPath() {
        DataNode dataNode = new DataNode();

        assertThrows(IllegalArgumentException.class, () -> dataNode.put(".value", null));
        assertThrows(IllegalArgumentException.class, () -> dataNode.put("node.", null));

        assertThrows(IllegalArgumentException.class, () -> dataNode.get(".value"));
        assertThrows(IllegalArgumentException.class, () -> dataNode.get("node."));
    }

    @Test
    public void simpleTest() {
        DataNode dataNode = new DataNode();

        dataNode.put("node1.value", "First value");
        dataNode.put("node1.node2.value", "Second value");

        assertEquals("First value", dataNode.get("node1.value"));
        assertEquals("Second value", dataNode.get("node1.node2.value"));
    }

    @Test
    public void testProtectedValue() {
        DataNode dataNode = new CustomDataNode();

        assertFalse(dataNode.put("protected-int", new ArrayList<>()));
        assertFalse(dataNode.put("protected-text", -1));
        assertTrue(dataNode.put("protected-node", DataNode.EMPTY));
    }

    @Test
    public void testProtectedPath() {
        CustomDataNode dataNode = new CustomDataNode();

        dataNode.exposedProtectValue("custom-node", DataNode.class, new CustomDataNode(), false);

        assertFalse(dataNode.put("node1.node2.protected-value.value", DataNode.EMPTY));
        assertFalse(dataNode.put("custom-node.protected-int", DataNode.EMPTY));
    }

    @Test
    public void testMapInfiltration() {
        DataNode dataNode = new DataNode();

        dataNode.put("map", new HashMap<>());

        //noinspection ConstantConditions
        assertNotEquals(dataNode.get("map").getClass(), HashMap.class);
    }

    private static class CustomDataNode extends DataNode {
        public CustomDataNode() {
            protectValue("protected-int", Integer.class, 1, false);
            protectValue("protected-text", String.class, "Im Protected!", false);
            protectValue("protected-node", DataNode.class, EMPTY, true);
            protectValue("node1.node2.protected-value", Integer.class, 1, false);
        }

        public final <S, T extends S> void exposedProtectValue(String key, Class<S> clazz, T value, boolean nullable) {
            protectValue(key, clazz, value, nullable);
        }
    }
}