package com.cutting.cuttingsystem.service;

import com.cutting.cuttingsystem.entitys.algorithm.PlaceSquare;
import com.cutting.cuttingsystem.entitys.algorithm.DTO.SolutionResponseDTO;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.buffer.BufferOp;
import org.locationtech.jts.operation.buffer.BufferParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class GCodeService {

    private static final Logger log = LoggerFactory.getLogger(GCodeService.class);

    private static final double SAFE_Z = 5.0;
    private static final int PLUNGE_FEED = 500;
    private static final double MITRE_LIMIT = 10.0;

    public record CutParams(double cutDepth, int cutFeed, int spindleSpeed) {
        public CutParams {
            if (cutDepth <= 0) cutDepth = 3.0;
            if (cutFeed <= 0) cutFeed = 3000;
            if (spindleSpeed <= 0) spindleSpeed = 18000;
        }
    }

    private final GeometryFactory geometryFactory = new GeometryFactory();

    /**
     * 为一组排版方案生成 NC 文件内容。
     * 每个 board 生成独立的 G-code 段，共享程序头尾。
     */
    public String generateNc(List<SolutionResponseDTO> solutions, double toolRadius, CutParams cutParams) {
        if (cutParams == null) cutParams = new CutParams(3.0, 3000, 18000);
        StringBuilder sb = new StringBuilder();
        appendHeader(sb, cutParams);

        int programNumber = 1001;
        int pieceIndex = 0;

        for (int boardIdx = 0; boardIdx < solutions.size(); boardIdx++) {
            SolutionResponseDTO solution = solutions.get(boardIdx);
            double boardW = solution.getContainerWidth();
            List<PlaceSquare> pieces = solution.getPlaceSquareList();
            if (pieces == null || pieces.isEmpty()) continue;

            sb.append(String.format(Locale.US, "(--- Board %d: %.0f x %.0f ---)%n",
                    boardIdx + 1, solution.getContainerLength(), boardW));

            for (PlaceSquare piece : pieces) {
                Coordinate[] offsetCoords = computeOffsetPath(piece, toolRadius, boardW);
                if (offsetCoords == null) {
                    log.warn("跳过件 {} ({}x{}): 尺寸过小，无法偏移刀具半径 {}",
                            piece.getId(), piece.getL(), piece.getW(), toolRadius);
                    continue;
                }
                appendPieceGcode(sb, offsetCoords, piece.getId(), ++pieceIndex, cutParams);
            }
        }

        appendFooter(sb);
        return sb.toString();
    }

    /**
     * 用 JTS buffer 计算刀具中心偏移路径。
     * 将算法坐标系（左上角原点，y 向下）转换为 CNC 坐标系（左下角原点，y 向上）。
     * 返回偏移后的坐标数组（闭合路径），或 null 表示件太小。
     */
    private Coordinate[] computeOffsetPath(PlaceSquare piece, double toolRadius, double boardWidth) {
        double x = piece.getX();
        double y = piece.getY();
        double l = piece.getL();
        double w = piece.getW();

        // 算法坐标系 -> CNC 坐标系（左下角原点）
        double cncY = boardWidth - y - w;

        // 构建原始矩形（CNC 坐标系）
        Coordinate[] coords = new Coordinate[]{
                new Coordinate(x, cncY),
                new Coordinate(x + l, cncY),
                new Coordinate(x + l, cncY + w),
                new Coordinate(x, cncY + w),
                new Coordinate(x, cncY) // 闭合环
        };

        Polygon polygon = geometryFactory.createPolygon(coords);

        // 向内偏移，使用 MITRE 连接保持尖角
        BufferParameters bufParams = new BufferParameters();
        bufParams.setJoinStyle(BufferParameters.JOIN_MITRE);
        bufParams.setMitreLimit(MITRE_LIMIT);

        Geometry offset = BufferOp.bufferOp(polygon, -toolRadius, bufParams);

        if (offset == null || offset.isEmpty() || !(offset instanceof Polygon)) {
            return null;
        }

        // 提取偏移后的外环坐标（去掉重复的闭合点）
        Coordinate[] offsetCoords = offset.getCoordinates();
        // JTS buffer 返回的坐标已闭合（首尾相同），去掉最后一个重复点
        if (offsetCoords.length > 1 &&
                offsetCoords[0].equals2D(offsetCoords[offsetCoords.length - 1])) {
            Coordinate[] trimmed = new Coordinate[offsetCoords.length - 1];
            System.arraycopy(offsetCoords, 0, trimmed, 0, trimmed.length);
            return trimmed;
        }
        return offsetCoords;
    }

    private void appendHeader(StringBuilder sb, CutParams cutParams) {
        sb.append("%\n");
        sb.append("O1001\n");
        sb.append("G21 G90 G17\n");
        sb.append("T1 M06\n");
        sb.append(String.format(Locale.US, "S%d M03\n", cutParams.spindleSpeed()));
        sb.append("G43 H1\n");
        sb.append(String.format(Locale.US, "G00 Z%.3f\n", SAFE_Z));
        sb.append("\n");
    }

    private void appendPieceGcode(StringBuilder sb, Coordinate[] coords, String pieceId, int index, CutParams cutParams) {
        if (coords.length < 2) return;

        sb.append(String.format(Locale.US, "(Piece %d: %s)%n", index, pieceId));

        // 快速定位到起点（安全高度）
        sb.append(String.format(Locale.US, "G00 X%.3f Y%.3f Z%.3f%n",
                coords[0].x, coords[0].y, SAFE_Z));

        // 下刀
        sb.append(String.format(Locale.US, "G01 Z%.3f F%d%n", -cutParams.cutDepth(), PLUNGE_FEED));

        // 切削各边
        sb.append(String.format(Locale.US, "G01 X%.3f Y%.3f F%d%n",
                coords[1].x, coords[1].y, cutParams.cutFeed()));
        for (int i = 2; i < coords.length; i++) {
            sb.append(String.format(Locale.US, "G01 X%.3f Y%.3f%n",
                    coords[i].x, coords[i].y));
        }
        // 回到起点闭合
        sb.append(String.format(Locale.US, "G01 X%.3f Y%.3f%n",
                coords[0].x, coords[0].y));

        // 抬刀
        sb.append(String.format(Locale.US, "G00 Z%.3f%n", SAFE_Z));
        sb.append("\n");
    }

    private void appendFooter(StringBuilder sb) {
        sb.append("M05\n");
        sb.append("M30\n");
        sb.append("%\n");
    }
}
