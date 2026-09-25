package com.oracle.productsapp.converters;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class FloatEmbeddingConverter
        implements AttributeConverter<float[], byte[]> {

    private static final ByteOrder BYTE_ORDER = ByteOrder.BIG_ENDIAN;

    @Override
    public byte[] convertToDatabaseColumn(float[] attribute) {
        return toBytes(attribute);
    }

    @Override
    public float[] convertToEntityAttribute(byte[] dbData) {
        return fromBytes(dbData);
    }

    public static byte[] toBytes(float[] embedding) {
        if (embedding == null) {
            return null;
        }

        ByteBuffer buffer = ByteBuffer
                .allocate(embedding.length * Float.BYTES)
                .order(BYTE_ORDER);

        for (float value : embedding) {
            buffer.putFloat(value);
        }

        return buffer.array();
    }

    public static float[] fromBytes(byte[] bytes) {
        if (bytes == null) {
            return null;
        }

        if (bytes.length % Float.BYTES != 0) {
            throw new IllegalArgumentException(
                    "Embedding BLOB length must be divisible by "
                            + Float.BYTES
            );
        }

        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(BYTE_ORDER);
        float[] embedding = new float[bytes.length / Float.BYTES];

        for (int index = 0; index < embedding.length; index++) {
            embedding[index] = buffer.getFloat();
        }

        return embedding;
    }
}
