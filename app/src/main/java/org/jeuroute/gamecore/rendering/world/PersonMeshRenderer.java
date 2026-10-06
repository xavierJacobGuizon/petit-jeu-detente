package org.jeuroute.gamecore.rendering.world;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;

import java.nio.FloatBuffer;
import java.util.List;
import java.util.Objects;
import org.jeuroute.model.records.camera.WorldViewBounds;
import org.jeuroute.model.world.settlement.Person;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL33;
import org.lwjgl.system.MemoryUtil;

public final class PersonMeshRenderer implements AutoCloseable {

	private static final int FLOATS_PER_VERTEX = 5;
	private static final int INITIAL_INSTANCE_CAPACITY = 1024;
	private static final int LOW_DETAIL_VERTEX_COUNT = 6;
	private static final int FULL_DETAIL_VERTEX_COUNT = 48;
	private static final int FULL_DETAIL_FIRST_VERTEX = LOW_DETAIL_VERTEX_COUNT;
	private static final int HEAD_SEGMENTS = 12;

	private static final String VERTEX_SHADER = """
	#version 330 compatibility
	layout(location = 0) in vec2 localPosition;
	layout(location = 1) in vec3 localColor;
	layout(location = 2) in vec2 instancePosition;
	out vec3 color;
	void main() {
	    vec2 worldPosition = instancePosition + localPosition;
	    gl_Position = gl_ModelViewProjectionMatrix * vec4(worldPosition, 0.0, 1.0);
	    color = localColor;
	}
	""";

	private static final String FRAGMENT_SHADER = """
	#version 330 compatibility
	in vec3 color;
	out vec4 fragmentColor;
	void main() {
	    fragmentColor = vec4(color, 1.0);
	}
	""";

	private int vertexArrayObject;
	private int meshBufferObject;
	private int instanceBufferObject;
	private int shaderProgram;
	private int instanceCapacity = INITIAL_INSTANCE_CAPACITY;
	private FloatBuffer instanceData;

	public PersonMeshRenderer() {
		if (!org.lwjgl.opengl.GL.getCapabilities().OpenGL33) {
			throw new IllegalStateException("Person instancing requires OpenGL 3.3");
		}
		instanceData = MemoryUtil.memAllocFloat(instanceCapacity * 2);
		try {
			initializeMesh();
		} catch (RuntimeException exception) {
			close();
			throw exception;
		}
	}

	public void render(List<Person> people, double zoom, WorldViewBounds viewBounds) {
		ensureInstanceCapacity(people.size());
		instanceData.clear();
		int visibleCount = 0;
		for (Person person : people) {
			if (
				person == null ||
				!viewBounds.contains(person.getPreciseX(), person.getPreciseY(), 10.0)
			) {
				continue;
			}
			instanceData.put((float) person.getPreciseX());
			instanceData.put((float) person.getPreciseY());
			visibleCount++;
		}
		if (visibleCount == 0) {
			return;
		}
		FloatBuffer uploadData = Objects.requireNonNull(instanceData);
		uploadData.flip();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, instanceBufferObject);
		GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0L, uploadData);
		GL20.glUseProgram(shaderProgram);
		GL30.glBindVertexArray(vertexArrayObject);
		if (zoom < 0.3) {
			GL31.glDrawArraysInstanced(GL_TRIANGLES, 0, LOW_DETAIL_VERTEX_COUNT, visibleCount);
		} else {
			GL31.glDrawArraysInstanced(
				GL_TRIANGLES,
				FULL_DETAIL_FIRST_VERTEX,
				FULL_DETAIL_VERTEX_COUNT,
				visibleCount
			);
		}
		GL30.glBindVertexArray(0);
		GL20.glUseProgram(0);
	}

	@Override
	public void close() {
		if (vertexArrayObject != 0) {
			GL30.glDeleteVertexArrays(vertexArrayObject);
			vertexArrayObject = 0;
		}
		if (meshBufferObject != 0) {
			GL15.glDeleteBuffers(meshBufferObject);
			meshBufferObject = 0;
		}
		if (instanceBufferObject != 0) {
			GL15.glDeleteBuffers(instanceBufferObject);
			instanceBufferObject = 0;
		}
		if (shaderProgram != 0) {
			GL20.glDeleteProgram(shaderProgram);
			shaderProgram = 0;
		}
		if (instanceData != null) {
			MemoryUtil.memFree(instanceData);
			instanceData = null;
		}
	}

	private void initializeMesh() {
		shaderProgram = createProgram();

		FloatBuffer meshData = MemoryUtil.memAllocFloat(
			(LOW_DETAIL_VERTEX_COUNT + FULL_DETAIL_VERTEX_COUNT) * FLOATS_PER_VERTEX
		);
		try {
			appendLowDetailMesh(meshData);
			appendFullDetailMesh(meshData);
			meshData.flip();

			vertexArrayObject = GL30.glGenVertexArrays();
			meshBufferObject = GL15.glGenBuffers();
			instanceBufferObject = GL15.glGenBuffers();
			GL30.glBindVertexArray(vertexArrayObject);

			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, meshBufferObject);
			GL15.glBufferData(GL15.GL_ARRAY_BUFFER, meshData, GL15.GL_STATIC_DRAW);
			GL20.glEnableVertexAttribArray(0);
			GL20.glVertexAttribPointer(0, 2, GL_FLOAT, false, FLOATS_PER_VERTEX * Float.BYTES, 0L);
			GL20.glEnableVertexAttribArray(1);
			GL20.glVertexAttribPointer(
				1,
				3,
				GL_FLOAT,
				false,
				FLOATS_PER_VERTEX * Float.BYTES,
				2L * Float.BYTES
			);

			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, instanceBufferObject);
			GL15.glBufferData(
				GL15.GL_ARRAY_BUFFER,
				(long) instanceCapacity * 2 * Float.BYTES,
				GL15.GL_DYNAMIC_DRAW
			);
			GL20.glEnableVertexAttribArray(2);
			GL20.glVertexAttribPointer(2, 2, GL_FLOAT, false, 2 * Float.BYTES, 0L);
			GL33.glVertexAttribDivisor(2, 1);
			GL30.glBindVertexArray(0);
		} finally {
			MemoryUtil.memFree(meshData);
		}
	}

	private void ensureInstanceCapacity(int requiredCapacity) {
		if (requiredCapacity <= instanceCapacity) {
			return;
		}
		while (instanceCapacity < requiredCapacity) {
			instanceCapacity = Math.multiplyExact(instanceCapacity, 2);
		}
		FloatBuffer expandedData = MemoryUtil.memAllocFloat(instanceCapacity * 2);
		MemoryUtil.memFree(instanceData);
		instanceData = expandedData;
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, instanceBufferObject);
		GL15.glBufferData(
			GL15.GL_ARRAY_BUFFER,
			(long) instanceCapacity * 2 * Float.BYTES,
			GL15.GL_DYNAMIC_DRAW
		);
	}

	private int createProgram() {
		int vertexShader = compileShader(GL20.GL_VERTEX_SHADER, VERTEX_SHADER);
		int fragmentShader = compileShader(GL20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
		int program = GL20.glCreateProgram();
		GL20.glAttachShader(program, vertexShader);
		GL20.glAttachShader(program, fragmentShader);
		GL20.glLinkProgram(program);
		GL20.glDeleteShader(vertexShader);
		GL20.glDeleteShader(fragmentShader);
		if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == 0) {
			String log = GL20.glGetProgramInfoLog(program);
			GL20.glDeleteProgram(program);
			throw new IllegalStateException("Unable to link person renderer shader: " + log);
		}
		return program;
	}

	private static int compileShader(int shaderType, String source) {
		int shader = GL20.glCreateShader(shaderType);
		GL20.glShaderSource(shader, new StringBuilder(source));
		GL20.glCompileShader(shader);
		if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == 0) {
			String log = GL20.glGetShaderInfoLog(shader);
			GL20.glDeleteShader(shader);
			throw new IllegalStateException("Unable to compile person renderer shader: " + log);
		}
		return shader;
	}

	private static void appendLowDetailMesh(FloatBuffer meshData) {
		appendTriangle(meshData, -3, -3, 3, -3, 3, 3, 0.22f, 0.48f, 0.78f);
		appendTriangle(meshData, -3, -3, 3, 3, -3, 3, 0.22f, 0.48f, 0.78f);
	}

	private static void appendFullDetailMesh(FloatBuffer meshData) {
		for (int segment = 0; segment < HEAD_SEGMENTS; segment++) {
			double firstAngle = (segment * Math.PI * 2.0) / HEAD_SEGMENTS;
			double secondAngle = ((segment + 1) * Math.PI * 2.0) / HEAD_SEGMENTS;
			appendTriangle(
				meshData,
				0,
				-5,
				4 * Math.cos(firstAngle),
				-5 + 4 * Math.sin(firstAngle),
				4 * Math.cos(secondAngle),
				-5 + 4 * Math.sin(secondAngle),
				0.92f,
				0.72f,
				0.38f
			);
		}
		appendTriangle(meshData, -3, -1, 3, -1, 4, 6, 0.22f, 0.48f, 0.78f);
		appendTriangle(meshData, -3, -1, 4, 6, -4, 6, 0.22f, 0.48f, 0.78f);
		appendTriangle(meshData, -2, 5, 0, 5, -2, 10, 0.16f, 0.18f, 0.2f);
		appendTriangle(meshData, 0, 5, 2, 5, 2, 10, 0.16f, 0.18f, 0.2f);
	}

	private static void appendTriangle(
		FloatBuffer target,
		double x1,
		double y1,
		double x2,
		double y2,
		double x3,
		double y3,
		float red,
		float green,
		float blue
	) {
		appendVertex(target, x1, y1, red, green, blue);
		appendVertex(target, x2, y2, red, green, blue);
		appendVertex(target, x3, y3, red, green, blue);
	}

	private static void appendVertex(
		FloatBuffer target,
		double x,
		double y,
		float red,
		float green,
		float blue
	) {
		target
			.put((float) x)
			.put((float) y)
			.put(red)
			.put(green)
			.put(blue);
	}
}
