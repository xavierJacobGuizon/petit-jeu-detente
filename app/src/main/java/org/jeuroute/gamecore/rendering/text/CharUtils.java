package org.jeuroute.gamecore.rendering.text;

import static org.lwjgl.opengl.GL11.*;

import java.util.HashMap;
import java.util.Map;
import org.jeuroute.model.records.presentation.TextRun;

public class CharUtils {

	private static final Map<Character, int[][]> GLYPH_CACHE = new HashMap<>();

	public static boolean isDigit(char c) {
		return c >= '0' && c <= '9';
	}

	public static boolean isLetter(char c) {
		return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
	}

	public static boolean isLetterOrDigit(char c) {
		return isLetter(c) || isDigit(c);
	}

	public static void drawNumber(int value, int x, int y, float r, float g, float b) {
		drawText(Integer.toString(Math.max(0, value)), x, y, r, g, b);
	}

	public static void drawText(String text, int x, int y, float r, float g, float b) {
		drawTextRuns(new TextRun(text, x, y, r, g, b, 2));
	}

	public static void drawGlyph(char glyph, int x, int y, float r, float g, float b) {
		drawTextRuns(new TextRun(Character.toString(glyph), x, y, r, g, b, 1));
	}

	/**
	 * Permet de dessiner du texte à l'écran
	 * Pour chaque caractère,
	 * la méthode récupère les segments de lignes correspondants
	 * et les dessine à l'écran.
	 * @param runs Texte à dessiner
	 */
	public static void drawTextRuns(TextRun... runs) {
		for (int runIndex = 0; runIndex < runs.length; runIndex++) {
			TextRun run = runs[runIndex];
			boolean widthAlreadyDrawn = false;
			for (int previousIndex = 0; previousIndex < runIndex; previousIndex++) {
				if (runs[previousIndex].police() == run.police()) {
					widthAlreadyDrawn = true;
					break;
				}
			}
			if (widthAlreadyDrawn) {
				continue;
			}

			glLineWidth(run.police());
			glBegin(GL_LINES);
			for (TextRun widthRun : runs) {
				if (widthRun.police() != run.police()) {
					continue;
				}
				glColor3f(widthRun.r(), widthRun.g(), widthRun.b());
				for (int index = 0; index < widthRun.text().length(); index++) {
					int[][] segments = glyphSegments(widthRun.text().charAt(index));
					if (segments == null) {
						continue;
					}
					int x = widthRun.x() + index * 16;
					for (int[] segment : segments) {
						glVertex2i(x + segment[0], widthRun.y() + segment[1]);
						glVertex2i(x + segment[2], widthRun.y() + segment[3]);
					}
				}
			}
			glEnd();
		}
		glLineWidth(1.0f);
	}

	/**
	 * Permet de créer des caractère dans l'affichage
	 *
	 * @param glyph
	 * @return
	 */
	private static int[][] glyphSegments(char glyph) {
		return GLYPH_CACHE.computeIfAbsent(glyph, key -> createGlyphSegments(key.charValue()));
	}

	private static int[][] createGlyphSegments(char glyph) {
		return switch (glyph) {
			case '0' -> new int[][] {
				{ 2, 0, 10, 0 },
				{ 10, 0, 10, 14 },
				{ 10, 14, 2, 14 },
				{ 2, 14, 2, 0 },
			};
			case '1' -> new int[][] { { 6, 0, 6, 14 } };
			case '2' -> new int[][] {
				{ 2, 0, 10, 0 },
				{ 10, 0, 10, 7 },
				{ 10, 7, 2, 7 },
				{ 2, 7, 2, 14 },
				{ 2, 14, 10, 14 },
			};
			case '3' -> new int[][] {
				{ 2, 0, 10, 0 },
				{ 10, 0, 10, 14 },
				{ 2, 7, 10, 7 },
				{ 2, 14, 10, 14 },
			};
			case '4' -> new int[][] { { 2, 0, 2, 7 }, { 2, 7, 10, 7 }, { 10, 0, 10, 14 } };
			case '5' -> new int[][] {
				{ 2, 0, 10, 0 },
				{ 2, 0, 2, 7 },
				{ 2, 7, 10, 7 },
				{ 10, 7, 10, 14 },
				{ 2, 14, 10, 14 },
			};
			case '6' -> new int[][] {
				{ 2, 0, 10, 0 },
				{ 2, 0, 2, 14 },
				{ 2, 7, 10, 7 },
				{ 10, 7, 10, 14 },
				{ 2, 14, 10, 14 },
			};
			case '7' -> new int[][] { { 2, 0, 10, 0 }, { 10, 0, 10, 14 } };
			case '8' -> new int[][] {
				{ 2, 0, 10, 0 },
				{ 10, 0, 10, 14 },
				{ 2, 14, 10, 14 },
				{ 2, 0, 2, 14 },
				{ 2, 7, 10, 7 },
			};
			case '9' -> new int[][] {
				{ 2, 0, 10, 0 },
				{ 10, 0, 10, 14 },
				{ 2, 0, 2, 7 },
				{ 2, 7, 10, 7 },
				{ 2, 14, 10, 14 },
			};
			case 'R' -> new int[][] {
				{ 2, 14, 2, 0 },
				{ 2, 0, 8, 0 },
				{ 8, 0, 8, 7 },
				{ 2, 7, 8, 7 },
				{ 2, 7, 10, 14 },
			};
			case 'I' -> new int[][] { { 6, 0, 6, 14 } };
			case 'V' -> new int[][] { { 2, 0, 6, 14 }, { 6, 14, 10, 0 } };
			case 'F' -> new int[][] { { 2, 0, 2, 14 }, { 2, 0, 10, 0 }, { 2, 7, 8, 7 } };
			case 'P' -> new int[][] {
				{ 2, 14, 2, 0 },
				{ 2, 0, 9, 0 },
				{ 9, 0, 9, 7 },
				{ 2, 7, 9, 7 },
			};
			case 'S' -> new int[][] {
				{ 10, 0, 2, 0 },
				{ 2, 0, 2, 7 },
				{ 2, 7, 10, 7 },
				{ 10, 7, 10, 14 },
				{ 10, 14, 2, 14 },
			};
			case 'O' -> new int[][] {
				{ 2, 0, 10, 0 },
				{ 10, 0, 10, 14 },
				{ 10, 14, 2, 14 },
				{ 2, 14, 2, 0 },
			};
			case 'N' -> new int[][] { { 2, 14, 2, 0 }, { 2, 0, 10, 14 }, { 10, 14, 10, 0 } };
			case 'M' -> new int[][] {
				{ 2, 14, 2, 0 },
				{ 2, 0, 6, 7 },
				{ 6, 7, 10, 0 },
				{ 10, 0, 10, 14 },
			};
			case 'E' -> new int[][] {
				{ 10, 0, 2, 0 },
				{ 2, 0, 2, 14 },
				{ 2, 7, 9, 7 },
				{ 2, 14, 10, 14 },
			};
			case 'U' -> new int[][] { { 2, 0, 2, 14 }, { 2, 14, 10, 14 }, { 10, 14, 10, 0 } };
			case 'T' -> new int[][] { { 2, 0, 10, 0 }, { 6, 0, 6, 14 } };
			case 'L' -> new int[][] { { 2, 0, 2, 14 }, { 2, 14, 10, 14 } };
			case 'C' -> new int[][] { { 10, 0, 2, 0 }, { 2, 0, 2, 14 }, { 2, 14, 10, 14 } };
			case 'H' -> new int[][] { { 2, 0, 2, 14 }, { 10, 0, 10, 14 }, { 2, 7, 10, 7 } };
			case 'A' -> new int[][] {
				{ 2, 14, 2, 2 },
				{ 2, 2, 10, 2 },
				{ 10, 2, 10, 14 },
				{ 2, 7, 10, 7 },
			};
			case 'B' -> new int[][] {
				{ 2, 0, 2, 14 },
				{ 2, 0, 9, 0 },
				{ 9, 0, 9, 7 },
				{ 2, 7, 9, 7 },
				{ 9, 7, 9, 14 },
				{ 2, 14, 9, 14 },
			};
			case 'D' -> new int[][] {
				{ 2, 0, 2, 14 },
				{ 2, 0, 8, 0 },
				{ 8, 0, 10, 2 },
				{ 10, 2, 10, 12 },
				{ 10, 12, 8, 14 },
				{ 8, 14, 2, 14 },
			};
			case 'G' -> new int[][] {
				{ 10, 2, 2, 2 },
				{ 2, 2, 2, 12 },
				{ 2, 12, 4, 14 },
				{ 4, 14, 10, 14 },
				{ 10, 14, 10, 8 },
				{ 10, 8, 6, 8 },
			};
			case 'J' -> new int[][] {
				{ 2, 0, 10, 0 },
				{ 10, 0, 10, 12 },
				{ 10, 12, 8, 14 },
				{ 8, 14, 3, 14 },
				{ 3, 14, 2, 12 },
			};
			case 'K' -> new int[][] { { 2, 0, 2, 14 }, { 10, 0, 2, 7 }, { 2, 7, 10, 14 } };
			case 'Q' -> new int[][] {
				{ 2, 0, 10, 0 },
				{ 10, 0, 10, 11 },
				{ 10, 11, 7, 14 },
				{ 7, 14, 2, 14 },
				{ 2, 14, 2, 0 },
				{ 7, 9, 11, 14 },
			};
			case 'W' -> new int[][] {
				{ 2, 0, 3, 14 },
				{ 3, 14, 6, 9 },
				{ 6, 9, 9, 14 },
				{ 9, 14, 10, 0 },
			};
			case 'X' -> new int[][] { { 2, 0, 10, 14 }, { 10, 0, 2, 14 } };
			case 'Y' -> new int[][] { { 2, 0, 6, 7 }, { 10, 0, 6, 7 }, { 6, 7, 6, 14 } };
			case 'Z' -> new int[][] { { 2, 0, 10, 0 }, { 10, 0, 2, 14 }, { 2, 14, 10, 14 } };
			case '.' -> new int[][] { { 6, 13, 7, 14 } };
			case '-' -> new int[][] { { 2, 7, 10, 7 } };
			default -> null;
		};
	}
}
