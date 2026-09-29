package org.jeuroute.utils;

import java.awt.Point;
import org.jeuroute.model.jouet.Vehicle;

public final class GeometryUtils {

	/**
	 * Calcule le point de fin d'un rectangle centré sur un point donné, en fonction
	 * de la taille du véhicule.
	 *
	 * @param vehicle Véhicule pour lequel le rectangle est calculé
	 * @param center  Point central du rectangle
	 * @return Point représentant le coin inférieur droit du rectangle
	 */
	public static Point rectangleEnd(Vehicle vehicle, Point center) {
		double half = vehicle.halfSize;
		return new Point((int) Math.round(center.x + half), (int) Math.round(center.y + half));
	}

	/**
	 * Calcule le point de départ d'un rectangle centré sur un point donné, en
	 * fonction de la taille du véhicule.
	 *
	 * @param vehicle Véhicule pour lequel le rectangle est calculé
	 * @param center  Point central du rectangle
	 * @return Point représentant le coin supérieur gauche du rectangle
	 */
	public static Point rectangleStart(org.jeuroute.model.jouet.Vehicle vehicle, Point center) {
		double half = vehicle.halfSize;
		return new Point((int) Math.round(center.x - half), (int) Math.round(center.y - half));
	}

	/**
	 * Vérifie si un point donné se trouve sur le segment défini par deux points.
	 *
	 * @param point Point à vérifier
	 * @param start Point de départ du segment
	 * @param end   Point d'arrivée du segment
	 * @return true si le point appartient au segment, false sinon
	 */
	public static boolean isPointOnSegment(Point point, Point start, Point end) {
		if (point == null || start == null || end == null) {
			return false;
		}

		double pointX = point.x - start.x;
		double pointY = point.y - start.y;
		double segmentX = end.x - start.x;
		double segmentY = end.y - start.y;
		double cross = crossProduct(pointX, pointY, segmentX, segmentY);
		double dot = dotProduct(pointX, pointY, segmentX, segmentY);

		if (Math.abs(cross) > 0.000001) {
			return false;
		}

		double lengthSquared = start.distanceSq(end);
		return dot >= 0.0 && dot <= lengthSquared;
	}

	/**
	 * Calcule le produit vectoriel de deux vecteurs définis par leurs composantes.
	 *
	 * @param firstX
	 * @param firstY
	 * @param secondX
	 * @param secondY
	 * @return
	 */
	public static double crossProduct(
		double firstX,
		double firstY,
		double secondX,
		double secondY
	) {
		return firstX * secondY - firstY * secondX;
	}

	/**
	 * Calcule le produit scalaire de deux vecteurs définis par leurs composantes.
	 *
	 * @param firstX
	 * @param firstY
	 * @param secondX
	 * @param secondY
	 * @return
	 */
	public static double dotProduct(double firstX, double firstY, double secondX, double secondY) {
		return firstX * secondX + firstY * secondY;
	}
}
