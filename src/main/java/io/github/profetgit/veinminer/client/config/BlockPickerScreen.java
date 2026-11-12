package io.github.profetgit.veinminer.client.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * The "Your blocks" picker: search any block (or a {@code #tag} inside a world), add or remove it from the custom block list.
 * Rows are a fixed pool of vanilla buttons moved over a hand-scrolled list, so the same code runs on every target. Changes
 * are returned to the settings screen it came from; that screen saves them with its other settings.
 */
public final class BlockPickerScreen extends Screen {
    private static final int ROW = 24;
    private static final int WIDTH = 308;
    private static final int MAX_RESULTS = 100;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GRAY = 0xFFA0A0A0;
    private static final int DIM = 0xFF707070;

    private enum Kind { ADD, ADDED, REMOVE, BLOCKED }

    private record Row(String entry, String name, String detail, ItemStack icon, Kind kind) {}

    private final VeinminerScreen parent;
    private final List<String> entries;
    private final List<BlockIndex.Entry> index = new ArrayList<>();
    private final List<Row> rows = new ArrayList<>();
    private final List<Button> buttons = new ArrayList<>();
    private EditBox search;
    private Button lookAt;
    private String query = "";
    private int scroll;
    private int left;
    private int listTop;
    private int listBottom;
    private int visible;

    BlockPickerScreen(VeinminerScreen parent, List<String> entries) {
        super(Component.literal("Your blocks"));
        this.parent = parent;
        this.entries = new ArrayList<>(entries);
        index.addAll(BlockIndex.blocks());
    }

    @Override
    protected void init() {
        left = width / 2 - WIDTH / 2;
        search = new EditBox(font, left, 22, WIDTH, 20, Component.literal("Search blocks"));
        search.setMaxLength(64);
        search.setHint(Component.literal("Search blocks or type #tag"));
        search.setValue(query);
        search.setResponder(text -> {
            query = text;
            scroll = 0;
            refresh();
        });
        addRenderableWidget(search);
        setInitialFocus(search);

        listTop = 46;
        listBottom = height - 34;
        visible = Math.max(1, (listBottom - listTop) / ROW);
        buttons.clear();
        for (int i = 0; i < visible; i++) {
            int slot = i;
            Button b = Button.builder(Component.empty(), x -> press(slot)).bounds(left + WIDTH - 62, listTop + i * ROW + 2, 60, 20).build();
            buttons.add(b);
            addRenderableWidget(b);
        }
        lookAt = Button.builder(Component.literal("Add the block I'm looking at"), b -> addLooked()).bounds(left, height - 28, 200, 20).build();
        addRenderableWidget(lookAt);
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).bounds(left + 204, height - 28, WIDTH - 204, 20).build());
        updateLookAt();
        refresh();
    }

    @Override
    public void tick() {
        updateLookAt();
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent.withBlocks(entries));
    }

    private Block looked() {
        if (minecraft == null || minecraft.level == null) return null;
        if (minecraft.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            return minecraft.level.getBlockState(hit.getBlockPos()).getBlock();
        }
        return null;
    }

    private void updateLookAt() {
        if (lookAt == null) return;
        Block block = looked();
        String tip;
        boolean on = false;
        if (block == null) tip = "Look at a block in your world first.";
        else if (BlockIndex.holdsItems(block)) tip = "Blocks that hold items never vein";
        else if (entries.contains(BlockIndex.idOf(block))) tip = BlockIndex.label(BlockIndex.idOf(block)) + " is already in your list.";
        else {
            on = true;
            tip = "Add " + block.getName().getString() + " to your list.";
        }
        lookAt.active = on;
        lookAt.setTooltip(Tooltip.create(Component.literal(tip)));
    }

    private void addLooked() {
        Block block = looked();
        if (block == null || BlockIndex.holdsItems(block)) return;
        String id = BlockIndex.idOf(block);
        if (!entries.contains(id)) entries.add(id);
        refresh();
        updateLookAt();
    }

    private void press(int slot) {
        int at = scroll + slot;
        if (at >= rows.size()) return;
        Row row = rows.get(at);
        switch (row.kind()) {
            case ADD -> entries.add(row.entry());
            case REMOVE -> entries.remove(row.entry());
            default -> {}
        }
        refresh();
        updateLookAt();
    }

    private void refresh() {
        rows.clear();
        String q = query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            for (String e : entries) rows.add(new Row(e, BlockIndex.label(e), e, BlockIndex.icon(e), Kind.REMOVE));
        } else if (q.startsWith("#")) {
            tagRows(q.substring(1));
        } else {
            blockRows(q);
        }
        scroll = Math.max(0, Math.min(scroll, rows.size() - visible));
        for (int i = 0; i < buttons.size(); i++) {
            Button b = buttons.get(i);
            int at = scroll + i;
            if (at >= rows.size()) {
                b.visible = false;
                continue;
            }
            Row row = rows.get(at);
            b.visible = true;
            b.setY(listTop + i * ROW + 2);
            b.active = row.kind() == Kind.ADD || row.kind() == Kind.REMOVE;
            b.setMessage(Component.literal(switch (row.kind()) {
                case REMOVE -> "Remove";
                case ADDED -> "Added";
                case BLOCKED -> "Can't";
                default -> "Add";
            }));
        }
    }

    private void blockRows(String q) {
        List<BlockIndex.Entry> exact = new ArrayList<>();
        List<BlockIndex.Entry> starts = new ArrayList<>();
        List<BlockIndex.Entry> other = new ArrayList<>();
        for (BlockIndex.Entry e : index) {
            if (!e.lower().contains(q) && !e.id().contains(q)) continue;
            String path = e.id().substring(e.id().indexOf(':') + 1);
            if (e.lower().equals(q) || path.equals(q) || e.id().equals(q)) exact.add(e);
            else if (e.lower().startsWith(q)) starts.add(e);
            else other.add(e);
        }
        List<BlockIndex.Entry> all = new ArrayList<>(exact);
        all.addAll(starts);
        all.addAll(other);
        for (BlockIndex.Entry e : all) {
            if (rows.size() >= MAX_RESULTS) break;
            Kind kind = entries.contains(e.id()) ? Kind.ADDED : BlockIndex.holdsItems(e.block()) ? Kind.BLOCKED : Kind.ADD;
            rows.add(new Row(e.id(), e.name(), e.id(), BlockIndex.icon(e.block()), kind));
        }
    }

    private void tagRows(String q) {
        List<BlockIndex.Tag> tags = BlockIndex.tags();
        if (tags.isEmpty()) {
            String id = q.contains(":") ? q : "minecraft:" + q;
            if (!q.isEmpty() && Identifier.tryParse(id) != null) {
                String entry = "#" + id;
                rows.add(new Row(entry, entry, "Checked in a world", ItemStack.EMPTY, entries.contains(entry) ? Kind.ADDED : Kind.ADD));
            }
            return;
        }
        for (BlockIndex.Tag t : tags) {
            if (rows.size() >= MAX_RESULTS) break;
            if (!t.id().contains(q)) continue;
            String entry = "#" + t.id();
            rows.add(new Row(entry, t.name() + " (tag)", entry + "  " + t.size() + " blocks", BlockIndex.icon(t.first()),
                entries.contains(entry) ? Kind.ADDED : Kind.ADD));
        }
    }

    private boolean scrolled(double amount) {
        if (rows.size() <= visible || amount == 0) return false;
        scroll = Math.max(0, Math.min(scroll - (int) Math.signum(amount), rows.size() - visible));
        refresh();
        return true;
    }

//? if >=1.21 {
    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        return scrolled(scrollY) || super.mouseScrolled(x, y, scrollX, scrollY);
    }
//?} else {
/*    @Override
    public boolean mouseScrolled(double x, double y, double delta) {
        return scrolled(delta) || super.mouseScrolled(x, y, delta);
    }
*///?}

//? if >=26.2 {
    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        panel(g);
        super.extractRenderState(g, mouseX, mouseY, partial);
        rows(g, mouseX, mouseY);
    }
//?} else {
/*    @Override
    public void render(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        Compat.background(this, g);
        panel(g);
        super.render(g, mouseX, mouseY, partial);
        rows(g, mouseX, mouseY);
    }
*///?}

    private void panel(GuiGraphicsExtractor g) {
        Compat.text(g, font, title.getString(), width / 2 - font.width(title.getString()) / 2, 8, WHITE);
        g.fill(left - 2, listTop, left + WIDTH + 10, listBottom, 0x90000000);
        g.fill(left - 2, listTop, left + WIDTH + 10, listTop + 1, 0xFF000000);
        g.fill(left - 2, listBottom - 1, left + WIDTH + 10, listBottom, 0xFF000000);
        if (rows.size() > visible) {
            int x = left + WIDTH + 2;
            int track = listBottom - listTop - 2;
            int thumb = Math.max(16, track * visible / rows.size());
            int y = listTop + 1 + (track - thumb) * scroll / Math.max(1, rows.size() - visible);
            g.fill(x, listTop + 1, x + 6, listBottom - 1, 0xFF000000);
            g.fill(x, y, x + 6, y + thumb, 0xFF808080);
            g.fill(x, y, x + 5, y + thumb - 1, 0xFFC0C0C0);
        }
    }

    private void rows(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (search.isFocused() && search.getValue().isEmpty()) {
            // EditBox hides its hint while focused, and the picker opens focused: draw it after the cursor
            Compat.text(g, font, "Search blocks or type #tag", search.getX() + 4 + font.width("_") + 2, search.getY() + (search.getHeight() - 8) / 2, 0xFF808080);
        }
        if (rows.isEmpty()) {
            String none = query.isBlank() ? "Nothing added yet" : query.trim().startsWith("#") ? "No tags match" : "No blocks match";
            Compat.text(g, font, none, width / 2 - font.width(none) / 2, listTop + 10, GRAY);
            return;
        }
        int textWidth = WIDTH - 62 - 28;
        for (int i = 0; i < visible && scroll + i < rows.size(); i++) {
            Row row = rows.get(scroll + i);
            int y = listTop + i * ROW;
            boolean grey = row.kind() == Kind.BLOCKED;
            boolean hover = mouseX >= left && mouseX < left + WIDTH && mouseY >= y && mouseY < y + ROW;
            if (hover) g.fill(left, y, left + WIDTH, y + ROW, 0x25FFFFFF);
            if (!row.icon().isEmpty()) Compat.item(g, row.icon(), left + 4, y + 4);
            Compat.text(g, font, fit(row.name(), textWidth), left + 26, y + 3, grey ? DIM : WHITE);
            Compat.text(g, font, fit(row.detail(), textWidth), left + 26, y + 13, grey ? DIM : GRAY);
            if (grey && hover) Compat.tooltip(g, font, Component.literal("Blocks that hold items never vein"), mouseX, mouseY);
        }
    }

    private String fit(String text, int max) {
        if (font.width(text) <= max) return text;
        String cut = text;
        while (cut.length() > 1 && font.width(cut + "...") > max) cut = cut.substring(0, cut.length() - 1);
        return cut + "...";
    }

    /** The search box, for the self-test. */
    public EditBox search() {
        return search;
    }

    /** The entries as they stand now, for the self-test. */
    public List<String> entries() {
        return List.copyOf(entries);
    }

    /** The row button of the visible row for this entry ({@code minecraft:netherrack}, {@code #minecraft:logs}), or null. */
    public Button button(String entry) {
        for (int i = 0; i < buttons.size() && scroll + i < rows.size(); i++) {
            if (rows.get(scroll + i).entry().equals(entry)) return buttons.get(i);
        }
        return null;
    }

    /** The footer button that adds the block under the crosshair, for the self-test. */
    public Button lookAtButton() {
        return lookAt;
    }
}
