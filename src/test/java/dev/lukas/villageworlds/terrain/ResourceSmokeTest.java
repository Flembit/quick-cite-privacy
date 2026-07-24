package dev.lukas.villageworlds.terrain;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.GZIPInputStream;

public final class ResourceSmokeTest {
    private static final Path DATA = Path.of("src/main/resources/data/villageworlds");

    private ResourceSmokeTest() {}

    public static void run() throws Exception {
        check(Files.isDirectory(DATA), "data namespace must exist");
        validateJsonFiles();
        validateRegistryReferences();
        validateTemplates();
    }

    private static void validateJsonFiles() throws IOException {
        try (var paths = Files.walk(DATA)) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".json")).toList()) {
                try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    JsonElement element = JsonParser.parseReader(reader);
                    check(element.isJsonObject(), path + " must contain a JSON object");
                }
            }
        }
    }

    private static void validateRegistryReferences() throws IOException {
        Path structureDir = DATA.resolve("worldgen/structure");
        Path setDir = DATA.resolve("worldgen/structure_set");
        Path poolDir = DATA.resolve("worldgen/template_pool");
        Path templateDir = DATA.resolve("structure");

        Set<String> structureIds = new HashSet<>();
        try (var paths = Files.walk(structureDir)) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".json")).toList()) {
                String id = relativeId(structureDir, path);
                structureIds.add(id);
                JsonObject structure = parse(path);
                check("minecraft:jigsaw".equals(string(structure, "type")), path + " must be jigsaw");
                String startPool = string(structure, "start_pool");
                check(startPool.startsWith("villageworlds:"), path + " start_pool must use the mod namespace");
                Path poolPath = poolDir.resolve(startPool.substring("villageworlds:".length()) + ".json");
                check(Files.isRegularFile(poolPath), "Missing pool " + poolPath + " referenced by " + path);
                check(structure.get("size").getAsInt() == 0, path + " must be a bounded single-piece structure");
                check(structure.has("project_start_to_heightmap"), path + " must project to the surface heightmap");
            }
        }

        int structureCount = structureIds.size();
        check(structureCount == 12, "Expected 12 custom structures, found " + structureCount);

        try (var paths = Files.walk(setDir)) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".json")).toList()) {
                JsonObject set = parse(path);
                JsonObject placement = set.getAsJsonObject("placement");
                int spacing = placement.get("spacing").getAsInt();
                int separation = placement.get("separation").getAsInt();
                check(spacing > separation, path + " spacing must exceed separation");
                JsonArray entries = set.getAsJsonArray("structures");
                check(entries.size() > 0, path + " must contain structures");
                for (JsonElement entry : entries) {
                    String structure = string(entry.getAsJsonObject(), "structure");
                    check(structure.startsWith("villageworlds:"), path + " must reference the mod namespace");
                    check(structureIds.contains(structure.substring("villageworlds:".length())),
                            path + " references missing structure " + structure);
                }
            }
        }

        try (var paths = Files.walk(poolDir)) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".json")).toList()) {
                JsonObject pool = parse(path);
                JsonArray elements = pool.getAsJsonArray("elements");
                check(elements.size() > 0, path + " must have at least one element");
                for (JsonElement weighted : elements) {
                    JsonObject weightedObject = weighted.getAsJsonObject();
                    check(weightedObject.get("weight").getAsInt() > 0, path + " weights must be positive");
                    JsonObject element = weightedObject.getAsJsonObject("element");
                    String location = string(element, "location");
                    check(location.startsWith("villageworlds:"), path + " location must use mod namespace");
                    Path template = templateDir.resolve(location.substring("villageworlds:".length()) + ".nbt");
                    check(Files.isRegularFile(template), path + " references missing template " + template);
                }
            }
        }
    }

    private static void validateTemplates() throws Exception {
        Path templateDir = DATA.resolve("structure");
        int count = 0;
        try (var paths = Files.walk(templateDir)) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".nbt")).toList()) {
                NbtSummary summary = readStructureSummary(path);
                check(summary.dataVersion() == 4671, path + " must target DataVersion 4671");
                check(summary.width() > 0 && summary.height() > 0 && summary.depth() > 0,
                        path + " must have a positive size");
                check(summary.paletteSize() >= 2, path + " must contain a real palette");
                check(summary.blockCount() == summary.width() * summary.height() * summary.depth(),
                        path + " must encode its complete structure volume");
                check(summary.width() <= 48 && summary.depth() <= 48 && summary.height() <= 24,
                        path + " exceeds bounded structure size limits");
                count++;
            }
        }
        check(count == 18, "Expected 18 NBT templates, found " + count);
    }

    private static NbtSummary readStructureSummary(Path path) throws Exception {
        try (InputStream raw = Files.newInputStream(path);
             DataInputStream input = new DataInputStream(new GZIPInputStream(raw))) {
            int rootType = input.readUnsignedByte();
            check(rootType == 10, path + " root tag must be a compound");
            input.readUTF();
            int width = -1;
            int height = -1;
            int depth = -1;
            int blockCount = -1;
            int paletteSize = -1;
            int dataVersion = -1;
            while (true) {
                int type = input.readUnsignedByte();
                if (type == 0) break;
                String name = input.readUTF();
                if (type == 9 && name.equals("size")) {
                    int elementType = input.readUnsignedByte();
                    int length = input.readInt();
                    check(elementType == 3 && length == 3, path + " size must be a list of three ints");
                    width = input.readInt();
                    height = input.readInt();
                    depth = input.readInt();
                } else if (type == 9 && name.equals("blocks")) {
                    int elementType = input.readUnsignedByte();
                    blockCount = input.readInt();
                    for (int i = 0; i < blockCount; i++) skipPayload(input, elementType);
                } else if (type == 9 && name.equals("palette")) {
                    int elementType = input.readUnsignedByte();
                    paletteSize = input.readInt();
                    for (int i = 0; i < paletteSize; i++) skipPayload(input, elementType);
                } else if (type == 3 && name.equals("DataVersion")) {
                    dataVersion = input.readInt();
                } else {
                    skipPayload(input, type);
                }
            }
            return new NbtSummary(width, height, depth, blockCount, paletteSize, dataVersion);
        }
    }

    private static void skipPayload(DataInputStream input, int type) throws IOException {
        switch (type) {
            case 0 -> { }
            case 1 -> input.readByte();
            case 2 -> input.readShort();
            case 3 -> input.readInt();
            case 4 -> input.readLong();
            case 5 -> input.readFloat();
            case 6 -> input.readDouble();
            case 7 -> input.skipNBytes(input.readInt());
            case 8 -> input.readUTF();
            case 9 -> {
                int elementType = input.readUnsignedByte();
                int length = input.readInt();
                for (int i = 0; i < length; i++) skipPayload(input, elementType);
            }
            case 10 -> {
                while (true) {
                    int childType = input.readUnsignedByte();
                    if (childType == 0) break;
                    input.readUTF();
                    skipPayload(input, childType);
                }
            }
            case 11 -> input.skipNBytes((long) input.readInt() * Integer.BYTES);
            case 12 -> input.skipNBytes((long) input.readInt() * Long.BYTES);
            default -> throw new IOException("Unknown NBT tag type " + type);
        }
    }

    private static JsonObject parse(Path path) throws IOException {
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static String string(JsonObject object, String name) {
        return object.get(name).getAsString();
    }

    private static String relativeId(Path root, Path file) {
        String value = root.relativize(file).toString().replace('\\', '/');
        return value.substring(0, value.length() - ".json".length());
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private record NbtSummary(int width, int height, int depth, int blockCount, int paletteSize, int dataVersion) {}
}
