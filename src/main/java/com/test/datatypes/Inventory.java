package com.test.datatypes;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.*;
import java.util.*;

public class Inventory {
    private String name;
    private Map<Material, Integer> materials;

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Inventory.class, new InventoryTypeAdapter())
            .registerTypeAdapter(Material.class, new MaterialAdapter())
            .setPrettyPrinting()
            .create();

    public Inventory(String name) {
        this.name = Objects.requireNonNull(name, "Inventory name cannot be null");
        this.materials = new LinkedHashMap<>();
    }

    public String getName() {
        return name;
    }

    public Map<Material, Integer> getMaterials() {
        return Collections.unmodifiableMap(materials);
    }

    public void addMaterial(Material material, int quantity) {
        Objects.requireNonNull(material, "Material cannot be null");
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
        materials.merge(material, quantity, Integer::sum);
    }

    public int getMaterialCount(Material material) {
        return materials.getOrDefault(material, 0);
    }

    public void updateMaterialQuantity(Material material, int quantity) {
        if (quantity <= 0) {
            materials.remove(material);
        } else {
            materials.put(material, quantity);
        }
    }

    public void removeMaterial(Material material) {
        materials.remove(material);
    }

    public void saveToFile(String path) throws IOException {
        try (Writer writer = new FileWriter(path)) {
            GSON.toJson(this, writer);
        }
    }

    public static Inventory loadFromFile(String path) throws IOException {
        try (Reader reader = new FileReader(path)) {
            return GSON.fromJson(reader, Inventory.class);
        }
    }

    private static class InventoryTypeAdapter extends TypeAdapter<Inventory> {

        @Override
        public void write(JsonWriter out, Inventory inventory) throws IOException {
            out.beginObject();
            out.name("name").value(inventory.name);
            out.name("materials");
            out.beginArray();
            for (Map.Entry<Material, Integer> entry : inventory.materials.entrySet()) {
                out.beginObject();
                out.name("material");
                // Delegate Material serialization to Gson
                GSON.getAdapter(Material.class).write(out, entry.getKey());
                out.name("quantity").value(entry.getValue());
                out.endObject();
            }
            out.endArray();
            out.endObject();
        }

        @Override
        public Inventory read(JsonReader in) throws IOException {
            String name = null;
            Map<Material, Integer> materials = new LinkedHashMap<>();

            in.beginObject();
            while (in.hasNext()) {
                String fieldName = in.nextName();
                if ("name".equals(fieldName)) {
                    name = in.nextString();
                } else if ("materials".equals(fieldName)) {
                    in.beginArray();
                    while (in.hasNext()) {
                        Material material = null;
                        int quantity = 0;
                        in.beginObject();
                        while (in.hasNext()) {
                            String prop = in.nextName();
                            if ("material".equals(prop)) {
                                material = GSON.getAdapter(Material.class).read(in);
                            } else if ("quantity".equals(prop)) {
                                quantity = in.nextInt();
                            } else {
                                in.skipValue();
                            }
                        }
                        in.endObject();
                        if (material != null) {
                            materials.put(material, quantity);
                        }
                    }
                    in.endArray();
                } else {
                    in.skipValue();
                }
            }
            in.endObject();

            Inventory inventory = new Inventory(name);
            inventory.materials = materials;
            return inventory;
        }
    }

    private static class MaterialAdapter implements JsonSerializer<Material>, JsonDeserializer<Material> {

        @Override
        public JsonElement serialize(Material material, java.lang.reflect.Type typeOfSrc, JsonSerializationContext context) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("materialName", material.getMaterialName());
            jsonObject.addProperty("materialId", material.getMaterialId());
            jsonObject.addProperty("materialType", material.getMaterialKind().name());
            return jsonObject;
        }

        @Override
        public Material deserialize(JsonElement json, java.lang.reflect.Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();
            String name = jsonObject.get("materialName").getAsString();
            int id = jsonObject.get("materialId").getAsInt();
            Kind kind = Kind.valueOf(jsonObject.get("materialType").getAsString());
            return new Material(name, id, kind);
        }
    }
}