package net.xenrao.playerplaces;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;

public class LocationRing {
	private static VertexBuffer vertexBuffer = null;

	public static VertexBuffer get() {
		if (vertexBuffer == null) {
			build();
		}
		return vertexBuffer;
	}

	private static void build() {
		BufferBuilder builder = Tesselator.getInstance().getBuilder();
		builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

		int segments = 64;
		double innerRadius = 0.85;
		double outerRadius = 1.0;
		double step = 360.0 / segments;

		for (int i = 0; i < segments; i++) {
			double a0 = Math.toRadians(i * step);
			double a1 = Math.toRadians((i + 1) * step);

			double cos0 = Math.cos(a0);
			double sin0 = Math.sin(a0);
			double cos1 = Math.cos(a1);
			double sin1 = Math.sin(a1);

			// Outer edge brighter, inner edge slightly darker
			int outerColor = 0xFFFFFFFF;
			int innerColor = 0xAAFFFFFF;

			// Quad: outer0, inner0, inner1, outer1
			builder.vertex(cos0 * outerRadius, 0, sin0 * outerRadius).color(outerColor).endVertex();
			builder.vertex(cos0 * innerRadius, 0, sin0 * innerRadius).color(innerColor).endVertex();
			builder.vertex(cos1 * innerRadius, 0, sin1 * innerRadius).color(innerColor).endVertex();
			builder.vertex(cos1 * outerRadius, 0, sin1 * outerRadius).color(outerColor).endVertex();
		}

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