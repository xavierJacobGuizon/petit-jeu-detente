package org.jeuroute.model.world;

public enum ResourceType {
	FOOD("Nourriture", 'N', 8.0, 20, 0.24f, 0.72f, 0.34f),
	WOOD("Bois", 'B', 12.0, 16, 0.62f, 0.39f, 0.20f),
	METAL("Métal", 'M', 16.0, 12, 0.55f, 0.65f, 0.70f);

	private final String label;
	private final char symbol;
	private final double productionIntervalSeconds;
	private final int storageCapacity;
	private final float red;
	private final float green;
	private final float blue;

	ResourceType(
		String label,
		char symbol,
		double productionIntervalSeconds,
		int storageCapacity,
		float red,
		float green,
		float blue
	) {
		this.label = label;
		this.symbol = symbol;
		this.productionIntervalSeconds = productionIntervalSeconds;
		this.storageCapacity = storageCapacity;
		this.red = red;
		this.green = green;
		this.blue = blue;
	}

	public String getLabel() {
		return label;
	}

	public char getSymbol() {
		return symbol;
	}

	public ResourceType getRequiredResourceType() {
		return switch (this) {
			case FOOD -> METAL;
			case WOOD -> FOOD;
			case METAL -> WOOD;
		};
	}

	public double getProductionIntervalSeconds() {
		return productionIntervalSeconds;
	}

	public int getStorageCapacity() {
		return storageCapacity;
	}

	public float getRed() {
		return red;
	}

	public float getGreen() {
		return green;
	}

	public float getBlue() {
		return blue;
	}
}
