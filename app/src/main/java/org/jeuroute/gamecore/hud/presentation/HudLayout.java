package org.jeuroute.gamecore.hud.presentation;

import java.util.ArrayList;
import java.util.List;
import org.jeuroute.model.records.configuration.enums.HudAnchor;
import org.jeuroute.gamecore.hud.elements.HudIndicatorSlot;
import org.jeuroute.model.records.hud.HudBounds;
import org.jeuroute.model.records.hud.HudDialogPlacement;
import org.jeuroute.model.records.hud.HudSlotPlacement;

public final class HudLayout {

	static final int SLOT_ITEM_WIDTH = 128;
	static final int SLOT_HEIGHT = 62;
	static final int BUTTON_WIDTH = 152;
	static final int BUTTON_HEIGHT = 38;
	static final int GAP = 8;
	static final int MARGIN = 16;
	static final int DIALOG_WIDTH = 440;
	static final int DIALOG_HEADER_HEIGHT = 54;
	static final int DIALOG_ROW_HEIGHT = 42;
	static final int DIALOG_PADDING = 12;

	private HudLayout() {}

	public static int dialogPageSize(int optionCount, int windowHeight) {
		int maxRows = Math.max(
			1,
			(windowHeight - MARGIN * 2 - DIALOG_HEADER_HEIGHT - DIALOG_PADDING * 2) /
				DIALOG_ROW_HEIGHT
		);
		return optionCount > maxRows ? Math.max(1, maxRows - 1) : maxRows;
	}

	public static HudDialogPlacement dialogPlacement(
		int optionCount,
		int page,
		int windowWidth,
		int windowHeight
	) {
		int pageSize = dialogPageSize(optionCount, windowHeight);
		int firstOption = page * pageSize;
		int visibleRows = Math.min(pageSize, Math.max(0, optionCount - firstOption));
		boolean paginated = optionCount > pageSize;
		int height =
			DIALOG_HEADER_HEIGHT +
			DIALOG_PADDING * 2 +
			(visibleRows + (paginated ? 1 : 0)) * DIALOG_ROW_HEIGHT;
		int x = (windowWidth - DIALOG_WIDTH) / 2;
		int y = (windowHeight - height) / 2;
		HudBounds panel = new HudBounds(x, y, DIALOG_WIDTH, height);
		List<HudBounds> options = new ArrayList<>();
		for (int index = 0; index < visibleRows; index++) {
			int optionX = x + DIALOG_PADDING;
			int optionY = y + DIALOG_HEADER_HEIGHT + DIALOG_PADDING + index * DIALOG_ROW_HEIGHT;
			options.add(
				new HudBounds(
					optionX,
					optionY,
					DIALOG_WIDTH - DIALOG_PADDING * 2,
					DIALOG_ROW_HEIGHT - 4
				)
			);
		}
		HudBounds previous = null;
		HudBounds next = null;
		if (paginated) {
			int navY = y + DIALOG_HEADER_HEIGHT + DIALOG_PADDING + visibleRows * DIALOG_ROW_HEIGHT;
			int navWidth = (DIALOG_WIDTH - DIALOG_PADDING * 2 - GAP) / 2;
			previous = new HudBounds(x + DIALOG_PADDING, navY, navWidth, DIALOG_ROW_HEIGHT - 4);
			next = new HudBounds(
				x + DIALOG_PADDING + navWidth + GAP,
				navY,
				navWidth,
				DIALOG_ROW_HEIGHT - 4
			);
		}
		return new HudDialogPlacement(panel, List.copyOf(options), previous, next);
	}

	public static List<HudSlotPlacement> layoutIndicatorSlots(
		List<HudIndicatorSlot> slots,
		int windowWidth,
		int windowHeight
	) {
		List<HudSlotPlacement> placements = new ArrayList<>();
		int usableWidth = windowWidth - MARGIN * 2;
		int usableHeight = windowHeight - MARGIN * 2;

		for (HudIndicatorSlot slot : slots) {
			int cellWidth = SLOT_ITEM_WIDTH;
			int width = Math.max(1, slot.getIndicators().size()) * cellWidth;
			int height = SLOT_HEIGHT;
			if (width > usableWidth || height > usableHeight) {
				throw new IllegalArgumentException(
					"HUD indicator slot does not fit inside the window"
				);
			}

			HudBounds placement = findPlacement(
				slot.getAnchor(),
				width,
				height,
				usableWidth,
				usableHeight,
				placements
			);
			if (placement == null) {
				throw new IllegalStateException(
					"No non-overlapping position is available for HUD indicator slot"
				);
			}
			placements.add(new HudSlotPlacement(slot, placement));
		}
		return List.copyOf(placements);
	}

	private static HudBounds findPlacement(
		HudAnchor anchor,
		int width,
		int height,
		int usableWidth,
		int usableHeight,
		List<HudSlotPlacement> occupied
	) {
		int horizontalStep = width + GAP;
		int verticalStep = height + GAP;
		for (
			int verticalOffset = 0;
			verticalOffset <= usableHeight - height;
			verticalOffset += verticalStep
		) {
			for (
				int horizontalOffset = 0;
				horizontalOffset <= usableWidth - width;
				horizontalOffset += horizontalStep
			) {
				int x = isRight(anchor)
					? MARGIN + usableWidth - width - horizontalOffset
					: MARGIN + horizontalOffset;
				int y = isBottom(anchor)
					? MARGIN + usableHeight - height - verticalOffset
					: MARGIN + verticalOffset;
				HudBounds candidate = new HudBounds(x, y, width, height);
				if (
					occupied.stream().noneMatch(existing -> candidate.intersects(existing.bounds()))
				) {
					return candidate;
				}
			}
		}
		return null;
	}

	private static boolean isRight(HudAnchor anchor) {
		return anchor == HudAnchor.TOP_RIGHT || anchor == HudAnchor.BOTTOM_RIGHT;
	}

	private static boolean isBottom(HudAnchor anchor) {
		return anchor == HudAnchor.BOTTOM_LEFT || anchor == HudAnchor.BOTTOM_RIGHT;
	}

	public static HudBounds rootButtonBounds(int index, int windowWidth, int windowHeight) {
		int x = windowWidth - MARGIN - BUTTON_WIDTH - index * (BUTTON_WIDTH + GAP);
		int y = windowHeight - MARGIN - BUTTON_HEIGHT;
		return new HudBounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT);
	}

	public static HudBounds submenuButtonBounds(
		int rootIndex,
		int submenuIndex,
		int windowWidth,
		int windowHeight
	) {
		HudBounds root = rootButtonBounds(rootIndex, windowWidth, windowHeight);
		int y = root.y() - GAP - BUTTON_HEIGHT - submenuIndex * (BUTTON_HEIGHT + GAP);
		return new HudBounds(root.x(), y, BUTTON_WIDTH, BUTTON_HEIGHT);
	}
}
