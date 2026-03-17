package net.xenrao.playerplaces;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;

public class LocationMark {
	private static VertexBuffer vertexBuffer = null;

	public static VertexBuffer get() {
		if (vertexBuffer == null) {
			build();
		}
		return vertexBuffer;
	}

	private static void build() {
		BufferBuilder builder = Tesselator.getInstance().getBuilder();
		builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
		int white = 0xFFFFFFFF;
		builder.vertex(0.5, 0.5, 0).uv(0, 0).color(white).endVertex();
		builder.vertex(0.5, -0.5, 0).uv(0, 1).color(white).endVertex();
		builder.vertex(-0.5, -0.5, 0).uv(1, 1).color(white).endVertex();
		builder.vertex(-0.5, 0.5, 0).uv(1, 0).color(white).endVertex();

		if (vertexBuffer != null) vertexBuffer.close();
		vertexBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
		vertexBuffer.bind();
		vertexBuffer.upload(builder.end());
		VertexBuffer.unbind();
	}

	public static void cleanup() {
		if (vertexBuffer != null) {
			vertexBuffer.close();
			vertexBuffer = null;
		}
	}
}