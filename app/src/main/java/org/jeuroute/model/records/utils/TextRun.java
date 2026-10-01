package org.jeuroute.model.records.utils;

public record TextRun(String text, int x, int y, float r, float g, float b, float police) {
	public static float POLICE_TXT = 2.0f;
	public static float POLICE_GLYPH = 2.0f;
}
