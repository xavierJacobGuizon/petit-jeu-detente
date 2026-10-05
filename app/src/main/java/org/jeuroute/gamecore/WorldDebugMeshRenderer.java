package org.jeuroute.gamecore;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_LINES;

import java.awt.Point;
import java.nio.FloatBuffer;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import org.jeuroute.gamecore.camera.WorldViewBounds;
import org.jeuroute.model.records.world.WorldRenderData;
import org.jeuroute.model.world.settlement.House;
import org.jeuroute.model.world.settlement.Person;
import org.jeuroute.model.world.transport.Vehicle;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL33;
import org.lwjgl.system.MemoryUtil;

public final class WorldDebugMeshRenderer implements AutoCloseable {

	private static final int FLOATS_PER_INSTANCE = 4;
	private static final int INSTANCE_STRIDE_BYTES = FLOATS_PER_INSTANCE * Float.BYTES;
	private static final int CIRCLE_SEGMENTS = 16;
	private static final double DESTINATION_RADIUS = 8.0;
	private static final int INITIAL_INSTANCE_CAPACITY = 1024;
	private static final int PERSON_LINES_PER_ZOOM_UNIT = 12_500;
	private static final float VEHICLE_RED = 0.95f;
	private static final float VEHICLE_GREEN = 0.72f;
	private static final float VEHICLE_BLUE = 0.2f;
	private static final float PERSON_RED = 0.25f;
	private static final float PERSON_GREEN = 0.9f;
	private static final float PERSON_BLUE = 0.75f;

	private static final String VERTEX_SHADER = """
	#version 330 compatibility
	layout(location = 0) in vec2 localPosition;
	layout(location = 1) in vec2 instanceOrigin;
	layout(location = 2) in vec2 instanceDestination;
	uniform int circleMode;
	uniform float circleRadius;
	uniform vec3 drawColor;
	out vec3 color;
	void main() {
	    vec2 worldPosition;
	    if (circleMode == 0) {
	        worldPosition = gl_VertexID == 0 ? instanceOrigin : instanceDestination;
	    } else {
	        worldPosition = instanceDestination + localPosition * circleRadius;
	    }
	    gl_Position = gl_ModelViewProjectionMatrix * vec4(worldPosition, 0.0, 1.0);
	    color = drawColor;
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

	private final Set<House> visibleDestinationHouses = Collections.newSetFromMap(
		new IdentityHashMap<>()
	);
	private int vertexArrayObject;
	private int meshBufferObject;
	private int lineInstanceBufferObject;
	private int circleInstanceBufferObject;
	private int shaderProgram;
	private int circleModeUniform;
	private int circleRadiusUniform;
	private int drawColorUniform;
	private int lineCapacity = INITIAL_INSTANCE_CAPACITY;
	private int circleCapacity = INITIAL_INSTANCE_CAPACITY;
	private int vehicleLineCount;
	private int personLineCount;
	private int vehicleCircleCount;
	private int houseCircleCount;
	private long personLineOffsetBytes;
	private long houseCircleOffsetBytes;
	private boolean hasPreparedDestinations;
	private FloatBuffer lineData;
	private FloatBuffer circleData;

	public WorldDebugMeshRenderer() {
		if (!org.lwjgl.opengl.GL.getCapabilities().OpenGL33) {
			throw new IllegalStateException("Debug instancing requires OpenGL 3.3");
		}
		lineData = MemoryUtil.memAllocFloat(lineCapacity * FLOATS_PER_INSTANCE);
		circleData = MemoryUtil.memAllocFloat(circleCapacity * FLOATS_PER_INSTANCE);
		try {
			initializeMesh();
		} catch (RuntimeException exception) {
			close();
			throw exception;
		}
	}

	public void renderDestinations(WorldRenderData world, double zoom, WorldViewBounds viewBounds) {
		updateDestinations(world, viewBounds, zoom);
		renderDestinations(zoom);
	}

	public void updateDestinations(WorldRenderData world, WorldViewBounds viewBounds, double zoom) {
		List<DebugRouteLineMerger.Line> routeLines =
			new PersonRouteGeometryCache().collectVisibleLines(world.people(), viewBounds);
		updateDestinations(world, viewBounds, zoom, routeLines);
	}

	public void updateDestinations(
		WorldRenderData world,
		WorldViewBounds viewBounds,
		double zoom,
		List<DebugRouteLineMerger.Line> routeLines
	) {
		int maximumPersonLines = Math.max(1, (int) (PERSON_LINES_PER_ZOOM_UNIT * zoom));
		int sampledPersonLineCount = Math.min(maximumPersonLines, routeLines.size());
		ensureLineCapacity(world.vehicles().size() + sampledPersonLineCount);
		ensureCircleCapacity(world.vehicles().size() + world.people().size());
		lineData.clear();
		circleData.clear();
		visibleDestinationHouses.clear();

		vehicleLineCount = appendVehicleLines(world, viewBounds);
		personLineOffsetBytes = lineData.position() * Float.BYTES;
		appendPersonLines(routeLines, maximumPersonLines);
		personLineCount = lineData.position() / FLOATS_PER_INSTANCE - vehicleLineCount;
		appendPersonDestinations(world, viewBounds);
		vehicleCircleCount = appendVehicleCircles(world, viewBounds);
		houseCircleOffsetBytes = circleData.position() * Float.BYTES;
		appendHouseCircles(viewBounds);
		houseCircleCount = circleData.position() / FLOATS_PER_INSTANCE - vehicleCircleCount;
		lineData.flip();
		circleData.flip();
		uploadInstanceData(lineInstanceBufferObject, lineData);
		uploadInstanceData(circleInstanceBufferObject, circleData);
		hasPreparedDestinations = true;
	}

	public void renderDestinations(double zoom) {
		if (!hasPreparedDestinations) {
			return;
		}
		GL11.glLineWidth((float) Math.max(1.0, 1.5 * zoom));
		GL20.glUseProgram(shaderProgram);
		GL30.glBindVertexArray(vertexArrayObject);
		if (vehicleLineCount > 0) {
			setInstanceAttributeOffsets(lineInstanceBufferObject, 0L);
			setLineUniforms(VEHICLE_RED, VEHICLE_GREEN, VEHICLE_BLUE);
			GL31.glDrawArraysInstanced(GL_LINES, 0, 2, vehicleLineCount);
		}
		if (personLineCount > 0) {
			setInstanceAttributeOffsets(lineInstanceBufferObject, personLineOffsetBytes);
			setLineUniforms(PERSON_RED, PERSON_GREEN, PERSON_BLUE);
			GL31.glDrawArraysInstanced(GL_LINES, 0, 2, personLineCount);
		}
		if (vehicleCircleCount > 0) {
			setInstanceAttributeOffsets(circleInstanceBufferObject, 0L);
			setCircleUniforms(VEHICLE_RED, VEHICLE_GREEN, VEHICLE_BLUE);
			GL31.glDrawArraysInstanced(GL_LINES, 2, CIRCLE_SEGMENTS * 2, vehicleCircleCount);
		}
		if (houseCircleCount > 0) {
			setInstanceAttributeOffsets(circleInstanceBufferObject, houseCircleOffsetBytes);
			setCircleUniforms(PERSON_RED, PERSON_GREEN, PERSON_BLUE);
			GL31.glDrawArraysInstanced(GL_LINES, 2, CIRCLE_SEGMENTS * 2, houseCircleCount);
		}
		GL30.glBindVertexArray(0);
		GL20.glUseProgram(0);
		GL11.glLineWidth(1.0f);
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
		if (lineInstanceBufferObject != 0) {
			GL15.glDeleteBuffers(lineInstanceBufferObject);
			lineInstanceBufferObject = 0;
		}
		if (circleInstanceBufferObject != 0) {
			GL15.glDeleteBuffers(circleInstanceBufferObject);
			circleInstanceBufferObject = 0;
		}
		if (shaderProgram != 0) {
			GL20.glDeleteProgram(shaderProgram);
			shaderProgram = 0;
		}
		if (lineData != null) {
			MemoryUtil.memFree(lineData);
			lineData = null;
		}
		if (circleData != null) {
			MemoryUtil.memFree(circleData);
			circleData = null;
		}
	}

	private int appendVehicleLines(WorldRenderData world, WorldViewBounds viewBounds) {
		int count = 0;
		for (Vehicle vehicle : world.vehicles()) {
			if (vehicle == null) {
				continue;
			}
			Point destination = vehicle.getDestination();
			if (
				destination != null &&
				viewBounds.intersectsSegment(
					vehicle.getPositionX(),
					vehicle.getPositionY(),
					destination.x,
					destination.y,
					0.75
				)
			) {
				putInstance(
					lineData,
					vehicle.getPositionX(),
					vehicle.getPositionY(),
					destination.x,
					destination.y
				);
				count++;
			}
		}
		return count;
	}

	private void appendPersonLines(
		List<DebugRouteLineMerger.Line> routeLines,
		int maximumPersonLines
	) {
		int sampledLineCount = Math.min(maximumPersonLines, routeLines.size());
		for (int sampleIndex = 0; sampleIndex < sampledLineCount; sampleIndex++) {
			int lineIndex = (int) (((long) sampleIndex * routeLines.size()) / sampledLineCount);
			DebugRouteLineMerger.Line line = routeLines.get(lineIndex);
			putInstance(lineData, line.startX(), line.startY(), line.endX(), line.endY());
		}
	}

	private void appendPersonDestinations(WorldRenderData world, WorldViewBounds viewBounds) {
		for (Person person : world.people()) {
			if (person == null) {
				continue;
			}
			House destinationHouse = person.getDestinationHouse();
			if (destinationHouse == null) {
				continue;
			}
			double destinationX = destinationHouse.getPositionX();
			double destinationY = destinationHouse.getPositionY();
			if (viewBounds.contains(destinationX, destinationY, DESTINATION_RADIUS)) {
				visibleDestinationHouses.add(destinationHouse);
			}
		}
	}

	private int appendVehicleCircles(WorldRenderData world, WorldViewBounds viewBounds) {
		int count = 0;
		for (Vehicle vehicle : world.vehicles()) {
			if (vehicle == null) {
				continue;
			}
			Point destination = vehicle.getDestination();
			if (
				destination != null &&
				viewBounds.contains(destination.x, destination.y, DESTINATION_RADIUS)
			) {
				putInstance(circleData, destination.x, destination.y, destination.x, destination.y);
				count++;
			}
		}
		return count;
	}

	private void appendHouseCircles(WorldViewBounds viewBounds) {
		for (House house : visibleDestinationHouses) {
			double x = house.getPositionX();
			double y = house.getPositionY();
			if (viewBounds.contains(x, y, DESTINATION_RADIUS)) {
				putInstance(circleData, x, y, x, y);
			}
		}
	}

	private void setLineUniforms(float red, float green, float blue) {
		GL20.glUniform1i(circleModeUniform, 0);
		GL20.glUniform3f(drawColorUniform, red, green, blue);
	}

	private void setCircleUniforms(float red, float green, float blue) {
		GL20.glUniform1i(circleModeUniform, 1);
		GL20.glUniform1f(circleRadiusUniform, (float) DESTINATION_RADIUS);
		GL20.glUniform3f(drawColorUniform, red, green, blue);
	}

	private void uploadInstanceData(int bufferObject, FloatBuffer data) {
		if (!data.hasRemaining()) {
			return;
		}
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bufferObject);
		GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0L, data);
	}

	private static void putInstance(
		FloatBuffer buffer,
		double originX,
		double originY,
		double destinationX,
		double destinationY
	) {
		buffer.put((float) originX);
		buffer.put((float) originY);
		buffer.put((float) destinationX);
		buffer.put((float) destinationY);
	}

	private void setInstanceAttributeOffsets(int bufferObject, long byteOffset) {
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bufferObject);
		GL20.glVertexAttribPointer(1, 2, GL_FLOAT, false, INSTANCE_STRIDE_BYTES, byteOffset);
		GL20.glVertexAttribPointer(
			2,
			2,
			GL_FLOAT,
			false,
			INSTANCE_STRIDE_BYTES,
			byteOffset + 2L * Float.BYTES
		);
	}

	private void ensureLineCapacity(int requiredInstances) {
		if (requiredInstances > lineCapacity) {
			lineCapacity = growCapacity(lineCapacity, requiredInstances);
			lineData = growFloatBuffer(lineData, lineCapacity);
			resizeInstanceBuffer(lineInstanceBufferObject, lineCapacity);
		}
	}

	private void ensureCircleCapacity(int requiredInstances) {
		if (requiredInstances > circleCapacity) {
			circleCapacity = growCapacity(circleCapacity, requiredInstances);
			circleData = growFloatBuffer(circleData, circleCapacity);
			resizeInstanceBuffer(circleInstanceBufferObject, circleCapacity);
		}
	}

	private static int growCapacity(int currentCapacity, int requiredCapacity) {
		int capacity = currentCapacity;
		while (capacity < requiredCapacity) {
			capacity = Math.multiplyExact(capacity, 2);
		}
		return capacity;
	}

	private static FloatBuffer growFloatBuffer(FloatBuffer previous, int capacity) {
		if (previous != null) {
			MemoryUtil.memFree(previous);
		}
		return MemoryUtil.memAllocFloat(capacity * FLOATS_PER_INSTANCE);
	}

	private static void resizeInstanceBuffer(int bufferObject, int capacity) {
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bufferObject);
		GL15.glBufferData(
			GL15.GL_ARRAY_BUFFER,
			(long) capacity * INSTANCE_STRIDE_BYTES,
			GL15.GL_DYNAMIC_DRAW
		);
	}

	private void initializeMesh() {
		shaderProgram = createProgram();
		circleModeUniform = GL20.glGetUniformLocation(shaderProgram, "circleMode");
		circleRadiusUniform = GL20.glGetUniformLocation(shaderProgram, "circleRadius");
		drawColorUniform = GL20.glGetUniformLocation(shaderProgram, "drawColor");

		FloatBuffer meshData = MemoryUtil.memAllocFloat((2 + CIRCLE_SEGMENTS * 2) * 2);
		try {
			meshData.put(0.0f).put(0.0f);
			meshData.put(1.0f).put(0.0f);
			for (int segment = 0; segment < CIRCLE_SEGMENTS; segment++) {
				int nextSegment = (segment + 1) % CIRCLE_SEGMENTS;
				double firstAngle = (Math.PI * 2.0 * segment) / CIRCLE_SEGMENTS;
				double secondAngle = (Math.PI * 2.0 * nextSegment) / CIRCLE_SEGMENTS;
				meshData.put((float) Math.cos(firstAngle)).put((float) Math.sin(firstAngle));
				meshData.put((float) Math.cos(secondAngle)).put((float) Math.sin(secondAngle));
			}
			meshData.flip();

			vertexArrayObject = GL30.glGenVertexArrays();
			meshBufferObject = GL15.glGenBuffers();
			lineInstanceBufferObject = GL15.glGenBuffers();
			circleInstanceBufferObject = GL15.glGenBuffers();
			GL30.glBindVertexArray(vertexArrayObject);
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, meshBufferObject);
			GL15.glBufferData(GL15.GL_ARRAY_BUFFER, meshData, GL15.GL_STATIC_DRAW);
			GL20.glEnableVertexAttribArray(0);
			GL20.glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * Float.BYTES, 0L);

			resizeInstanceBuffer(lineInstanceBufferObject, lineCapacity);
			resizeInstanceBuffer(circleInstanceBufferObject, circleCapacity);
			GL20.glEnableVertexAttribArray(1);
			GL20.glEnableVertexAttribArray(2);
			setInstanceAttributeOffsets(lineInstanceBufferObject, 0L);
			GL33.glVertexAttribDivisor(1, 1);
			GL33.glVertexAttribDivisor(2, 1);
			GL30.glBindVertexArray(0);
		} finally {
			MemoryUtil.memFree(meshData);
		}
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
			throw new IllegalStateException("Unable to link debug renderer shader: " + log);
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
			throw new IllegalStateException("Unable to compile debug renderer shader: " + log);
		}
		return shader;
	}
}
