package us.dot.its.jpo.ode.plugin.j2735.oss;

import com.oss.asn1.Coder;
import com.oss.asn1.EncodeFailedException;
import com.oss.asn1.EncodeNotSupportedException;
import us.dot.its.jpo.ode.j2735.J2735;
import us.dot.its.jpo.ode.j2735.dsrc.*;
import us.dot.its.jpo.ode.j2735.dsrc.GeographicalPath.Description;
import us.dot.its.jpo.ode.j2735.dsrc.TravelerDataFrame.Content;
import us.dot.its.jpo.ode.j2735.dsrc.TravelerDataFrame.MsgId;
import us.dot.its.jpo.ode.j2735.dsrc.TravelerDataFrame.Regions;
import us.dot.its.jpo.ode.j2735.dsrc.ValidRegion.Area;
import us.dot.its.jpo.ode.j2735.itis.ITIScodesAndText;
import us.dot.its.jpo.ode.plugin.j2735.J2735Position3D;
import us.dot.its.jpo.ode.plugin.j2735.J2735TravelerInputData;
import us.dot.its.jpo.ode.util.CodecUtils;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class OssTravelerMessageBuilder {
   public static TravelerInformation travelerInfo;
   private static final SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm a", Locale.ENGLISH);

   public TravelerInformation buildTravelerInformation(J2735TravelerInputData travInputData)
         throws ParseException, EncodeFailedException, EncodeNotSupportedException {

      travelerInfo = new TravelerInformation();
      validateMessageCount(travInputData.tim.MsgCount);
      travelerInfo.setMsgCnt(new MsgCount(travInputData.tim.MsgCount));
      ByteBuffer buf = ByteBuffer.allocate(9).put((byte)0).putLong(travInputData.tim.UniqueMSGID);
      travelerInfo.setPacketID(new UniqueMSGID(buf.array()));
      validateURL(travInputData.tim.urlB);
      travelerInfo.setUrlB(new URL_Base(travInputData.tim.urlB));
      travelerInfo.setDataFrames(buildDataFrames(travInputData));

      return travelerInfo;
   }

   private TravelerDataFrameList buildDataFrames(J2735TravelerInputData travInputData) throws ParseException {
      TravelerDataFrameList dataFrames = new TravelerDataFrameList();

      validateFrameCount(travInputData.tim.dataframes.length);
      int len = travInputData.tim.dataframes.length;
      for (int i = 0; i < len; i++) {
         J2735TravelerInputData.DataFrame inputDataFrame = travInputData.tim.dataframes[i];
         TravelerDataFrame dataFrame = new TravelerDataFrame();

         // Part I, header
         validateHeaderIndex(inputDataFrame.sspTimRights);
         dataFrame.setSspTimRights(new SSPindex(inputDataFrame.sspTimRights));
         validateInfoType(inputDataFrame.frameType);
         dataFrame.setFrameType(TravelerInfoType.valueOf(inputDataFrame.frameType));
         dataFrame.setMsgId(getMessageId(inputDataFrame));
         dataFrame.setStartYear(new DYear(getStartYear(inputDataFrame)));
         dataFrame.setStartTime(new MinuteOfTheYear(getStartTime(inputDataFrame)));
         validateMinutesDuration(inputDataFrame.durationTime);
         dataFrame.setDuratonTime(new MinutesDuration(inputDataFrame.durationTime));
         validateSign(inputDataFrame.priority);
         dataFrame.setPriority(new SignPrority(inputDataFrame.priority));

         // -- Part II, Applicable Regions of Use
         validateHeaderIndex(inputDataFrame.sspLocationRights);
         dataFrame.setSspLocationRights(new SSPindex(inputDataFrame.sspLocationRights));
         dataFrame.setRegions(buildRegions(inputDataFrame.regions));

         // -- Part III, Content
         validateHeaderIndex(inputDataFrame.sspMsgTypes);
         dataFrame.setSspMsgRights1(new SSPindex(inputDataFrame.sspMsgTypes)); // allowed
                                                                               // message
                                                                               // types
         validateHeaderIndex(inputDataFrame.sspMsgContent);
         dataFrame.setSspMsgRights2(new SSPindex(inputDataFrame.sspMsgContent)); // allowed
                                                                                 // message
                                                                                 // content
         dataFrame.setContent(buildContent(inputDataFrame));
         validateURLShort(inputDataFrame.url);
         dataFrame.setUrl(new URL_Short(inputDataFrame.url));
         dataFrames.add(dataFrame);
      }
      return dataFrames;
   }

   public String getHexTravelerInformation()
         throws EncodeFailedException, EncodeNotSupportedException {
      Coder coder = J2735.getPERUnalignedCoder();
      ByteArrayOutputStream sink = new ByteArrayOutputStream();
      coder.encode(travelerInfo, sink);
      byte[] bytes = sink.toByteArray();
      return CodecUtils.toHex(bytes);
   }

   private Content buildContent(J2735TravelerInputData.DataFrame inputDataFrame) {
      String contentType = inputDataFrame.content;
      String[] codes = inputDataFrame.items;
      Content content = new Content();
      if ("Advisory".equals(contentType)) {
         content.setAdvisory(buildAdvisory(codes));
      } else if ("Work Zone".equals(contentType)) {
         content.setWorkZone(buildWorkZone(codes));
      } else if ("Speed Limit".equals(contentType)) {
         content.setSpeedLimit(buildSpeedLimit(codes));
      } else if ("Exit Service".equals(contentType)) {
         content.setExitService(buildExitService(codes));
      } else {
         content.setGenericSign(buildGenericSignage(codes));
      }
      return content;
   }

   private ITIScodesAndText buildAdvisory(String[] codes) {
      ITIScodesAndText itisText = new ITIScodesAndText();
      for (String code : codes) {
         validateITISCodes(code);
         ITIScodesAndText.Sequence_ seq = new ITIScodesAndText.Sequence_();
         ITIScodesAndText.Sequence_.Item item = new ITIScodesAndText.Sequence_.Item();
         item.setItis(Long.parseLong(code));
         seq.setItem(item);
         itisText.add(seq);
      }
      return itisText;
   }

   private WorkZone buildWorkZone(String[] codes) {
      WorkZone wz = new WorkZone();
      for (String code : codes) {
         validateContentCodes(code);
         WorkZone.Sequence_ seq = new WorkZone.Sequence_();
         WorkZone.Sequence_.Item item = new WorkZone.Sequence_.Item();
         item.setItis(Long.parseLong(code));
         seq.setItem(item);
         wz.add(seq);
      }
      return wz;
   }

   private SpeedLimit buildSpeedLimit(String[] codes) {
      SpeedLimit sl = new SpeedLimit();
      for (String code : codes) {
         validateContentCodes(code);
         SpeedLimit.Sequence_ seq = new SpeedLimit.Sequence_();
         SpeedLimit.Sequence_.Item item = new SpeedLimit.Sequence_.Item();
         item.setItis(Long.parseLong(code));
         seq.setItem(item);
         sl.add(seq);
      }
      return sl;
   }

   private ExitService buildExitService(String[] codes) {
      ExitService es = new ExitService();
      for (String code : codes) {
         validateContentCodes(code);
         ExitService.Sequence_ seq = new ExitService.Sequence_();
         ExitService.Sequence_.Item item = new ExitService.Sequence_.Item();
         item.setItis(Long.parseLong(code));
         seq.setItem(item);
         es.add(seq);
      }
      return es;
   }

   private GenericSignage buildGenericSignage(String[] codes) {
      GenericSignage gs = new GenericSignage();
      for (String code : codes) {
         validateContentCodes(code);
         GenericSignage.Sequence_ seq = new GenericSignage.Sequence_();
         GenericSignage.Sequence_.Item item = new GenericSignage.Sequence_.Item();
         item.setItis(Long.parseLong(code));
         seq.setItem(item);
         gs.add(seq);
      }
      return gs;
   }

   private MsgId getMessageId(J2735TravelerInputData.DataFrame dataFrame) {
      MsgId msgId = new MsgId();
      validateMessageID(dataFrame.msgID);

      if ("RoadSignID".equals(dataFrame.msgID)) {
         msgId.setChosenFlag(MsgId.roadSignID_chosen);
         RoadSignID roadSignID = new RoadSignID();
         validateLong(dataFrame.longitude);
         validateLat(dataFrame.latitude);
         validateElevation(dataFrame.elevation);
         roadSignID.setPosition(getPosition3D(dataFrame.latitude, dataFrame.longitude, dataFrame.elevation));
         validateHeading(dataFrame.viewAngle);
         roadSignID.setViewAngle(getHeadingSlice(dataFrame.viewAngle));
         validateMUTCDCode(dataFrame.mutcd);
         roadSignID.setMutcdCode(MUTCDCode.valueOf(dataFrame.mutcd));
         // roadSignID.setCrc(new MsgCRC(new byte[] { 0xC0,0x2F })); //Causing
         // error while encoding
         msgId.setRoadSignID(roadSignID);
      } else {
         msgId.setChosenFlag(MsgId.furtherInfoID_chosen);
         msgId.setFurtherInfoID(new FurtherInfoID(new byte[] { 0x00, 0x00 })); // TODO
                                                                               // check
                                                                               // this
                                                                               // for
                                                                               // actual
                                                                               // value
      }
      return msgId;
   }

   private Position3D getPosition3D(long latitude, long longitude, long elevation) {
      validateLat(latitude);
      validateLong(longitude);
      validateElevation(elevation);
      J2735Position3D position = new J2735Position3D(latitude, longitude, elevation);
      return OssPosition3D.position3D(position);
   }

   private HeadingSlice getHeadingSlice(String heading) {
      if (heading == null || heading.length() == 0) {
         return new HeadingSlice(new byte[] { 0x00, 0x00 });
      } else {
         short result = 0;
         for (int i = 0; i < 16; i++) {
            if (heading.charAt(i) == '1') {
               result |= 1;
            }
            result <<= 1;
         }
         return new HeadingSlice(ByteBuffer.allocate(2).putShort(result).array());
      }
   }

   private Regions buildRegions(J2735TravelerInputData.DataFrame.Region[] inputRegions) {
      Regions regions = new Regions();
      for (J2735TravelerInputData.DataFrame.Region inputRegion : inputRegions) {
         GeographicalPath geoPath = new GeographicalPath();
         Description description = new Description();
         validateGeoName(inputRegion.name);
         geoPath.setName(new DescriptiveName(inputRegion.name));
         validateRoadID(inputRegion.regulatorID);
         validateRoadID(inputRegion.segmentID);
         geoPath.setId(new RoadSegmentReferenceID(new RoadRegulatorID(inputRegion.regulatorID),
               new RoadSegmentID(inputRegion.segmentID)));
         geoPath
               .setAnchor(getPosition3D(inputRegion.anchor_lat, inputRegion.anchor_long, inputRegion.anchor_elevation));
         validateLaneWidth(inputRegion.laneWidth);
         geoPath.setLaneWidth(new LaneWidth(inputRegion.laneWidth));
         validateDirectionality(inputRegion.directionality);
         geoPath.setDirectionality(new DirectionOfUse(inputRegion.directionality));
         geoPath.setClosedPath(Boolean.valueOf(inputRegion.closedPath));
         validateHeading(inputRegion.direction);
         geoPath.setDirection(getHeadingSlice(inputRegion.direction));

         if ("path".equals(inputRegion.description)) {
            OffsetSystem offsetSystem = new OffsetSystem();
            validateZoom(inputRegion.path.scale);
            offsetSystem.setScale(new Zoom(inputRegion.path.scale));
            if ("xy".equals(inputRegion.path.type)) {
               if (inputRegion.path.nodes.length > 0){

                  offsetSystem.setOffset(new OffsetSystem.Offset());
                  offsetSystem.offset.setXy(buildNodeXYList(inputRegion.path.nodes));
               } else {
                  offsetSystem.setOffset(new OffsetSystem.Offset());
                  offsetSystem.offset.setXy(buildComputedLane(inputRegion.path.computedLane));
               }
            } else if ("ll".equals(inputRegion.path.type)) {
               if (inputRegion.path.nodes.length > 0) {
                  offsetSystem.setOffset(new OffsetSystem.Offset());
                  offsetSystem.offset.setLl(buildNodeLLList(inputRegion.path.nodes));
               }
            }
            description.setPath(offsetSystem);
            geoPath.setDescription(description);
         } else if ("geometry".equals(inputRegion.description)) {
            GeometricProjection geo = new GeometricProjection();
            validateHeading(inputRegion.geometry.direction);
            geo.setDirection(getHeadingSlice(inputRegion.geometry.direction));
            validateExtent(inputRegion.geometry.extent);
            geo.setExtent(new Extent(inputRegion.geometry.extent));
            validateLaneWidth(inputRegion.geometry.laneWidth);
            geo.setLaneWidth(new LaneWidth(inputRegion.geometry.laneWidth));
            geo.setCircle(buildGeoCircle(inputRegion.geometry));
            description.setGeometry(geo);
            geoPath.setDescription(description);

         } else { // oldRegion
            ValidRegion validRegion = new ValidRegion();
            validateHeading(inputRegion.oldRegion.direction);
            validRegion.setDirection(getHeadingSlice(inputRegion.oldRegion.direction));
            validateExtent(inputRegion.oldRegion.extent);
            validRegion.setExtent(new Extent(inputRegion.oldRegion.extent));
            Area area = new Area();
            if ("shapePointSet".equals(inputRegion.oldRegion.area)) {
               ShapePointSet sps = new ShapePointSet();
               sps.setAnchor(getPosition3D(inputRegion.oldRegion.shapepoint.latitude,
                     inputRegion.oldRegion.shapepoint.longitude, inputRegion.oldRegion.shapepoint.elevation));
               validateLaneWidth(inputRegion.oldRegion.shapepoint.laneWidth);
               sps.setLaneWidth(new LaneWidth(inputRegion.oldRegion.shapepoint.laneWidth));
               validateDirectionality(inputRegion.oldRegion.shapepoint.directionality);
               sps.setDirectionality(new DirectionOfUse(inputRegion.oldRegion.shapepoint.directionality));
               // nodeList NodeListXY, ADD HEREif
               // ("xy".equals(inputRegion.path.type)) {
               if (inputRegion.oldRegion.shapepoint.nodexy.length > 0) {
                  sps.setNodeList(buildNodeXYList(inputRegion.oldRegion.shapepoint.nodexy));
               } else {
                  sps.setNodeList(buildComputedLane(inputRegion.oldRegion.shapepoint.computedLane));
               }
               area.setShapePointSet(sps);
               validRegion.setArea(area);
            } else if ("regionPointSet".equals(inputRegion.oldRegion.area)) {
               RegionPointSet rps = new RegionPointSet();
               rps.setAnchor(getPosition3D(inputRegion.oldRegion.regionPoint.latitude,
                     inputRegion.oldRegion.regionPoint.longitude, inputRegion.oldRegion.regionPoint.elevation));
               validateZoom(inputRegion.oldRegion.regionPoint.scale);
               rps.setScale(new Zoom(inputRegion.oldRegion.regionPoint.scale));
               RegionList rl = buildRegionOffsets(inputRegion.oldRegion.regionPoint.regionList);
               rps.setNodeList(rl);
               area.setRegionPointSet(rps);
               validRegion.setArea(area);
            } else {// circle
               area.setCircle(buildOldCircle(inputRegion.oldRegion));
               validRegion.setArea(area);
            }
            description.setOldRegion(validRegion);
            geoPath.setDescription(description);
         }
         regions.add(geoPath);
      }
      return regions;
   }

   private RegionList buildRegionOffsets(J2735TravelerInputData.DataFrame.Region.OldRegion.RegionPoint.RegionList[] list) {
      RegionList myList = new RegionList();
      for (int i = 0; i < list.length; i++) {
         RegionOffsets ele = new RegionOffsets();
         validatex16Offset(list[i].xOffset);
         ele.setXOffset(new OffsetLL_B16(list[i].xOffset));
         validatey16Offset(list[i].yOffset);
         ele.setYOffset(new OffsetLL_B16(list[i].yOffset));
         validatez16Offset(list[i].zOffset);
         ele.setZOffset(new OffsetLL_B16(list[i].zOffset));
         myList.add(ele);
      }
      return myList;
   }

   private Circle buildGeoCircle(J2735TravelerInputData.DataFrame.Region.Geometry geo) {
      Circle circle = new Circle();
      circle.setCenter(getPosition3D(geo.circle.latitude, geo.circle.longitude, geo.circle.elevation));
      validateRadius(geo.circle.radius);
      circle.setRadius(new Radius_B12(geo.circle.radius));
      validateUnits(geo.circle.units);
      circle.setUnits(new DistanceUnits(geo.circle.units));
      return circle;
   }

   private Circle buildOldCircle(J2735TravelerInputData.DataFrame.Region.OldRegion reg) {
      Circle circle = new Circle();
      circle.setCenter(getPosition3D(reg.circle.latitude, reg.circle.longitude, reg.circle.elevation));
      validateRadius(reg.circle.radius);
      circle.setRadius(new Radius_B12(reg.circle.radius));
      validateUnits(reg.circle.units);
      circle.setUnits(new DistanceUnits(reg.circle.units));
      return circle;
   }

   private NodeListXY buildNodeXYList(J2735TravelerInputData.NodeXY[] inputNodes) {
      NodeListXY nodeList = new NodeListXY();
      NodeSetXY nodes = new NodeSetXY();
      for (int i = 0; i < inputNodes.length; i++) {
         J2735TravelerInputData.NodeXY point = inputNodes[i];

         NodeXY node = new NodeXY();
         NodeOffsetPointXY nodePoint = new NodeOffsetPointXY();

            if ("node-XY1".equals(point.delta)) {
               Node_XY_20b xy = new Node_XY_20b(new Offset_B10(point.x), new Offset_B10(point.y));
               nodePoint.setNode_XY1(xy);
            }

            if ("node-XY2".equals(point.delta)) {
               Node_XY_22b xy = new Node_XY_22b(new Offset_B11(point.x), new Offset_B11(point.y));
               nodePoint.setNode_XY2(xy);
            }

            if ("node-XY3".equals(point.delta)) {
               Node_XY_24b xy = new Node_XY_24b(new Offset_B12(point.x), new Offset_B12(point.y));
               nodePoint.setNode_XY3(xy);
            }

            if ("node-XY4".equals(point.delta)) {
               Node_XY_26b xy = new Node_XY_26b(new Offset_B13(point.x), new Offset_B13(point.y));
               nodePoint.setNode_XY4(xy);
            }

            if ("node-XY5".equals(point.delta)) {
               Node_XY_28b xy = new Node_XY_28b(new Offset_B14(point.x), new Offset_B14(point.y));
               nodePoint.setNode_XY5(xy);
            }

            if ("node-XY6".equals(point.delta)) {
               Node_XY_32b xy = new Node_XY_32b(new Offset_B16(point.x), new Offset_B16(point.y));
               nodePoint.setNode_XY6(xy);
            }

            if ("node-LatLon".equals(point.delta)) {
               Node_LLmD_64b nodeLatLong = new Node_LLmD_64b(new Longitude(point.nodeLat), new Latitude(point.nodeLong));
               nodePoint.setNode_LatLon(nodeLatLong);
            }

            node.setDelta(nodePoint);
         if (!point.attributes.equals("")){
            NodeAttributeSetXY attributes = new NodeAttributeSetXY();

            if (point.attributes.localNodes.length >0 ) {
               NodeAttributeXYList localNodeList = new NodeAttributeXYList();
               for (J2735TravelerInputData.LocalNode localNode : point.attributes.localNodes) {
                  localNodeList.add(new NodeAttributeXY(localNode.type));
               }
               attributes.setLocalNode(localNodeList);
            }

            if (point.attributes.disabledLists.length > 0) {
               SegmentAttributeXYList disabledNodeList = new SegmentAttributeXYList();
               for (J2735TravelerInputData.DisabledList disabledList : point.attributes.disabledLists) {
                  disabledNodeList.add(new SegmentAttributeXY(disabledList.type));
               }
               attributes.setDisabled(disabledNodeList);
            }

            if (point.attributes.enabledLists.length > 0 ) {
               SegmentAttributeXYList enabledNodeList = new SegmentAttributeXYList();
               for (J2735TravelerInputData.EnabledList enabledList : point.attributes.enabledLists) {
                  enabledNodeList.add(new SegmentAttributeXY(enabledList.type));
               }
               attributes.setEnabled(enabledNodeList);
            }

            if (point.attributes.dataLists.length > 0) {
               LaneDataAttributeList dataNodeList = new LaneDataAttributeList();
               for (J2735TravelerInputData.DataList dataList : point.attributes.dataLists) {

                  LaneDataAttribute dataAttribute = new LaneDataAttribute();

                  dataAttribute.setPathEndPointAngle(new DeltaAngle(dataList.pathEndpointAngle));
                  dataAttribute.setLaneCrownPointCenter(new RoadwayCrownAngle(dataList.laneCrownCenter));
                  dataAttribute.setLaneCrownPointLeft(new RoadwayCrownAngle(dataList.laneCrownLeft));
                  dataAttribute.setLaneCrownPointRight(new RoadwayCrownAngle(dataList.laneCrownRight));
                  dataAttribute.setLaneAngle(new MergeDivergeNodeAngle(dataList.laneAngle));

                  SpeedLimitList speedDataList = new SpeedLimitList();
                  for (J2735TravelerInputData.SpeedLimits speedLimit : dataList.speedLimits) {
                     speedDataList.add(
                             new RegulatorySpeedLimit(new SpeedLimitType(speedLimit.type), new Velocity(speedLimit.velocity)));
                  }

                  dataAttribute.setSpeedLimits(speedDataList);
                  dataNodeList.add(dataAttribute);
               }

               attributes.setData(dataNodeList);
            }

            attributes.setDWidth(new Offset_B10(point.attributes.dWidth));
            attributes.setDElevation(new Offset_B10(point.attributes.dElevation));

            node.setAttributes(attributes);


         }
         nodes.add(node);
      }

      nodeList.setNodes(nodes);
      return nodeList;
   }

   private NodeListXY buildComputedLane(J2735TravelerInputData.ComputedLane inputLane) {
      NodeListXY nodeList = new NodeListXY();

      ComputedLane computedLane = new ComputedLane();

      computedLane.setReferenceLaneId(new LaneID(inputLane.laneID));
      if (inputLane.offsetLargeX > 0) {
         computedLane.offsetXaxis.setLarge(inputLane.offsetLargeX);
      } else {
         computedLane.offsetXaxis.setSmall(inputLane.offsetSmallX);
      }

      if (inputLane.offsetLargeX > 0) {
         computedLane.offsetYaxis.setLarge(inputLane.offsetLargeY);

      } else {
         computedLane.offsetYaxis.setSmall(inputLane.offsetSmallY);
      }
      computedLane.setRotateXY(new Angle(inputLane.angle));
      computedLane.setScaleXaxis(new Scale_B12(inputLane.xScale));
      computedLane.setScaleYaxis(new Scale_B12(inputLane.yScale));

      nodeList.setComputed(computedLane);
      return nodeList;
   }

   private NodeListLL buildNodeLLList(J2735TravelerInputData.NodeXY[] inputNodes) {
      NodeListLL nodeList = new NodeListLL();
      NodeSetLL nodes = new NodeSetLL();
      for (int i = 0; i < inputNodes.length; i++) {
         J2735TravelerInputData.NodeXY point = inputNodes[i];

         NodeLL node = new NodeLL();
         NodeOffsetPointLL nodePoint = new NodeOffsetPointLL();

         if ("node-LL1".equals(point.delta)) {
            Node_LL_24B xy = new Node_LL_24B(new OffsetLL_B12(point.nodeLat), new OffsetLL_B12(point.nodeLong));
            nodePoint.setNode_LL1(xy);
         }

         if ("node-LL2".equals(point.delta)) {
            Node_LL_28B xy = new Node_LL_28B(new OffsetLL_B14(point.nodeLat), new OffsetLL_B14(point.nodeLong));
            nodePoint.setNode_LL2(xy);
         }

         if ("node-LL3".equals(point.delta)) {
            Node_LL_32B xy = new Node_LL_32B(new OffsetLL_B16(point.nodeLat), new OffsetLL_B16(point.nodeLong));
            nodePoint.setNode_LL3(xy);
         }

         if ("node-LL4".equals(point.delta)) {
            Node_LL_36B xy = new Node_LL_36B(new OffsetLL_B18(point.nodeLat), new OffsetLL_B18(point.nodeLong));
            nodePoint.setNode_LL4(xy);
         }

         if ("node-LL5".equals(point.delta)) {
            Node_LL_44B xy = new Node_LL_44B(new OffsetLL_B22(point.nodeLat), new OffsetLL_B22(point.nodeLong));
            nodePoint.setNode_LL5(xy);
         }

         if ("node-LL6".equals(point.delta)) {
            Node_LL_48B xy = new Node_LL_48B(new OffsetLL_B24(point.nodeLat), new OffsetLL_B24(point.nodeLong));
            nodePoint.setNode_LL6(xy);
         }

         if ("node-LatLon".equals(point.delta)) {
            Node_LLmD_64b nodeLatLong = new Node_LLmD_64b(new Longitude(point.nodeLat), new Latitude(point.nodeLong));
            nodePoint.setNode_LatLon(nodeLatLong);
         }
         node.setDelta(nodePoint);

         NodeAttributeSetLL attributes = new NodeAttributeSetLL();

         if (point.attributes.localNodes.length >0 ) {
            NodeAttributeLLList localNodeList = new NodeAttributeLLList();
            for (J2735TravelerInputData.LocalNode localNode : point.attributes.localNodes) {
               localNodeList.add(new NodeAttributeLL(localNode.type));
            }
            attributes.setLocalNode(localNodeList);
         }

         if (point.attributes.disabledLists.length > 0) {
            SegmentAttributeLLList disabledNodeList = new SegmentAttributeLLList();
            for (J2735TravelerInputData.DisabledList disabledList : point.attributes.disabledLists) {
               disabledNodeList.add(new SegmentAttributeLL(disabledList.type));
            }
            attributes.setDisabled(disabledNodeList);
         }

         if (point.attributes.enabledLists.length > 0 ) {
            SegmentAttributeLLList enabledNodeList = new SegmentAttributeLLList();
            for (J2735TravelerInputData.EnabledList enabledList : point.attributes.enabledLists) {
               enabledNodeList.add(new SegmentAttributeLL(enabledList.type));
            }
            attributes.setEnabled(enabledNodeList);
         }

         if (point.attributes.dataLists.length > 0) {
            LaneDataAttributeList dataNodeList = new LaneDataAttributeList();
            for (J2735TravelerInputData.DataList dataList : point.attributes.dataLists) {

               LaneDataAttribute dataAttribute = new LaneDataAttribute();

               dataAttribute.setPathEndPointAngle(new DeltaAngle(dataList.pathEndpointAngle));
               dataAttribute.setLaneCrownPointCenter(new RoadwayCrownAngle(dataList.laneCrownCenter));
               dataAttribute.setLaneCrownPointLeft(new RoadwayCrownAngle(dataList.laneCrownLeft));
               dataAttribute.setLaneCrownPointRight(new RoadwayCrownAngle(dataList.laneCrownRight));
               dataAttribute.setLaneAngle(new MergeDivergeNodeAngle(dataList.laneAngle));

               SpeedLimitList speedDataList = new SpeedLimitList();
               for (J2735TravelerInputData.SpeedLimits speedLimit : dataList.speedLimits) {
                  speedDataList.add(
                          new RegulatorySpeedLimit(new SpeedLimitType(speedLimit.type), new Velocity(speedLimit.velocity)));
               }

               dataAttribute.setSpeedLimits(speedDataList);
               dataNodeList.add(dataAttribute);
            }

            attributes.setData(dataNodeList);
         }

         attributes.setDWidth(new Offset_B10(point.attributes.dWidth));
         attributes.setDElevation(new Offset_B10(point.attributes.dElevation));

         node.setAttributes(attributes);

         nodes.add(node);
      }
      nodeList.setNodes(nodes);
      return nodeList;
   }

   private long getStartTime(J2735TravelerInputData.DataFrame dataFrame) throws ParseException {
      Date startDate = sdf.parse(dataFrame.startTime);
      String startOfYearTime = "01/01/" + getStartYear(dataFrame) + " 12:00 AM";
      Date startOfYearDate = sdf.parse(startOfYearTime);
      long minutes = (startDate.getTime() - startOfYearDate.getTime()) / 60000;
      validateStartTime(minutes);
      return minutes;
   }

   private int getStartYear(J2735TravelerInputData.DataFrame dataFrame) throws ParseException {
      Date startDate = sdf.parse(dataFrame.startTime);
      Calendar cal = Calendar.getInstance();
      cal.setTime(startDate);
      validateStartYear(Calendar.YEAR);
      return cal.get(Calendar.YEAR);
   }

   public static void validateMessageCount(int msg) {
      if (msg > 127 || msg < 0)
         throw new IllegalArgumentException("Invalid message count");
   }

   public static void validateURL(String url) {
      if (url.isEmpty())
         throw new IllegalArgumentException("Invalid empty url");
      if (url.length() < 1 || url.length() > 45)
         throw new IllegalArgumentException("Invalid URL provided");
   }

   public static void validateURLShort(String url) {
      if (url.isEmpty())
         throw new IllegalArgumentException("Invalid empty Short url");
      if (url.length() < 1 || url.length() > 15)
         throw new IllegalArgumentException("Invalid URL provided");
   }

   public static void validateFrameCount(int count) {
      if (count < 1 || count > 8)
         throw new IllegalArgumentException("Invalid number of dataFrames");
   }

   public static void validateMessageID(String str) {
      validateString(str);
      if (!("RoadSignID").equals(str) && !("furtherInfoID").equals(str))
         throw new IllegalArgumentException("Invalid messageID");
   }

   public static void validateStartYear(int year) {
      if (year < 0 || year > 4095)
         throw new IllegalArgumentException("Not a valid start year");
   }

   public static void validateStartTime(long time) {
      if (time < 0 || time > 527040)
         throw new IllegalArgumentException("Invalid start Time");
   }

   public static void validateMinutesDuration(long dur) {
      if (dur < 0 || dur > 32000)
         throw new IllegalArgumentException("Invalid Duration");
   }

   public static void validateHeaderIndex(short count) {
      if (count < 0 || count > 31)
         throw new IllegalArgumentException("Invalid header sspIndex");
   }

   public static void validateInfoType(int num) {
      if (num < 0)
         throw new IllegalArgumentException("Invalid enumeration");
   }

   public static void validateLat(long lat) {
      if (lat < -900000000 || lat > 900000001)
         throw new IllegalArgumentException("Invalid Latitude");
   }

   public static void validateLong(long lonng) {
      if (lonng < -1799999999 || lonng > 1800000001)
         throw new IllegalArgumentException("Invalid Longitude");
   }

   public static void validateElevation(long elev) {
      if (elev < -4096 || elev > 61439)
         throw new IllegalArgumentException("Invalid Elevation");
   }

   public static void validateHeading(String head) {
      validateString(head);
      if (head.length() != 16) {
         throw new IllegalArgumentException("Invalid BitString");
      }
   }

   public static void validateMUTCDCode(int mutc) {
      if (mutc < 0 || mutc > 6)
         throw new IllegalArgumentException("Invalid Enumeration");
   }

   public static void validateSign(int sign) {
      if (sign < 0 || sign > 7)
         throw new IllegalArgumentException("Invalid Sign Priority");
   }

   public static void validateITISCodes(String code) {
      int cd;
      try {
         cd = Integer.parseInt(code);
         if (cd < 0 || cd > 65535)
            throw new IllegalArgumentException("Invalid ITIS code");
      } catch (NumberFormatException e) {
         if (code.isEmpty())
            throw new IllegalArgumentException("Invalid empty string");
         if (code.length() < 1 || code.length() > 500)
            throw new IllegalArgumentException("Invalid test Phrase");
      }
   }

   public static void validateContentCodes(String code) {
      int cd;
      try {
         cd = Integer.parseInt(code);
         if (cd < 0 || cd > 65535)
            throw new IllegalArgumentException("Invalid ITIS code");
      } catch (NumberFormatException e) {
         if (code.isEmpty())
            throw new IllegalArgumentException("Invalid empty string");
         if (code.length() < 1 || code.length() > 16)
            throw new IllegalArgumentException("Invalid test Phrase");
      }
   }

   public static void validateString(String str) {
      if (str.isEmpty())
         throw new IllegalArgumentException("Invalid Empty String");
   }

   public static void validateGeoName(String name) {
      if (name.length() < 1 || name.length() > 63)
         throw new IllegalArgumentException("Invalid Descriptive name");
   }

   public static void validateRoadID(int id) {
      if (id < 0 || id > 65535)
         throw new IllegalArgumentException("Invalid RoadID");
   }

   public static void validateLaneWidth(int width) {
      if (width < 0 || width > 32767)
         throw new IllegalArgumentException("Invalid lane width");
   }

   public static void validateDirectionality(long dir) {
      if (dir < 0 || dir > 3)
         throw new IllegalArgumentException("Invalid enumeration");
   }

   public static void validateZoom(int z) {
      if (z < 0 || z > 15)
         throw new IllegalArgumentException("Invalid zoom");
   }

   public static void validateExtent(int ex) {
      if (ex < 0 || ex > 15)
         throw new IllegalArgumentException("Invalid extent enumeration");
   }

   public static void validateRadius(int rad) {
      if (rad < 0 || rad > 4095)
         throw new IllegalArgumentException("Invalid radius");
   }

   public static void validateUnits(int unit) {
      if (unit < 0 || unit > 7)
         throw new IllegalArgumentException("Invalid units enumeration");
   }

   public static void validatex16Offset(int x) {
      if (x < -32768 || x > 32767)
         throw new IllegalArgumentException("Invalid x offset");
   }

   public static void validatey16Offset(int y) {
      if (y < -32768 || y > 32767)
         throw new IllegalArgumentException("Invalid y offset");
   }

   public static void validatez16Offset(int z) {
      if (z < -32768 || z > 32767)
         throw new IllegalArgumentException("Invalid z offset");
   }

   public static void validateB10Offset(int b) {
      if (b < -512 || b > 511)
         throw new IllegalArgumentException("Invalid B10_Offset");
   }

   public static void validateB11Offset(int b) {
      if (b < -1024 || b > 1023)
         throw new IllegalArgumentException("Invalid B11_Offset");
   }

   public static void validateB12Offset(int b) {
      if (b < -2048 || b > 2047)
         throw new IllegalArgumentException("Invalid B12_Offset");
   }

   public static void validateB13Offset(int b) {
      if (b < -4096 || b > 4095)
         throw new IllegalArgumentException("Invalid B13_Offset");
   }

   public static void validateB14Offset(int b) {
      if (b < -8192 || b > 8191)
         throw new IllegalArgumentException("Invalid B14_Offset");
   }

   public static void validateB16Offset(int b) {
      if (b < -32768 || b > 32767)
         throw new IllegalArgumentException("Invalid B16_Offset");
   }

   public static void validateLL12Offset(int b) {
      if (b < -2048 || b > 2047)
         throw new IllegalArgumentException("Invalid B10_Offset");
   }

   public static void validateLL14Offset(int b) {
      if (b < -8192 || b > 8191)
         throw new IllegalArgumentException("Invalid B11_Offset");
   }

   public static void validateLL16Offset(int b) {
      if (b < -32768 || b > 32767)
         throw new IllegalArgumentException("Invalid B12_Offset");
   }

   public static void validateLL18Offset(int b) {
      if (b < -131072 || b > 131071)
         throw new IllegalArgumentException("Invalid B13_Offset");
   }

   public static void validateLL22Offset(int b) {
      if (b < -2097152 || b > 2097151)
         throw new IllegalArgumentException("Invalid B14_Offset");
   }

   public static void validateLL24Offset(int b) {
      if (b < -8388608 || b > 8388607)
         throw new IllegalArgumentException("Invalid B16_Offset");
   }

   public static void validateLaneID(int lane) {
      if (lane < 0 || lane > 255)
         throw new IllegalArgumentException("Invalid LaneID");
   }

   public static void validateSmallDrivenLine(int line) {
      if (line < -2047 || line > 2047)
         throw new IllegalArgumentException("Invalid Small Offset");
   }

   public static void validateLargeDrivenLine(int line) {
      if (line < -32767 || line > 32767)
         throw new IllegalArgumentException("Invalid Large Offset");
   }

   public static void validateAngle(int ang) {
      if (ang < 0 || ang > 28800)
         throw new IllegalArgumentException("Invalid Angle");
   }

   public static void validateB12Scale(int b) {
      if (b < -2048 || b > 2047)
         throw new IllegalArgumentException("Invalid B12 Scale");
   }

   public static void validateNodeAttribute(String str) {
      String myString = "reserved stopLine roundedCapStyleA roundedCapStyleB mergePoint divergePoint downstreamStopLine donwstreamStartNode closedToTraffic safeIsland curbPresentAtStepOff hydrantPresent";
      CharSequence cs = str;
      if (myString.contains(cs)) {
         return;
      } else {
         throw new IllegalArgumentException("Invalid NodeAttribute Enumeration");
      }
   }
   
   public static void validateSegmentAttribute(String str) {
      String myString = "reserved doNotBlock whiteLine mergingLaneLeft mergingLaneRight curbOnLeft curbOnRight loadingzoneOnLeft loadingzoneOnRight turnOutPointOnLeft turnOutPointOnRight adjacentParkingOnLeft adjacentParkingOnRight sharedBikeLane bikeBoxInFront transitStopOnLeft transitStopOnRight transitStopInLane sharedWithTrackedVehicle safeIsland lowCurbsPresent rumbleStripPresent audibleSignalingPresent adaptiveTimingPresent rfSignalRequestPresent partialCurbIntrusion taperToLeft taperToRight taperToCenterLine parallelParking headInParking freeParking timeRestrictionsOnParking costToPark midBlockCurbPresent unEvenPavementPresent";
      CharSequence cs = str;
      if (myString.contains(cs)) {
         return;
      } else {
         throw new IllegalArgumentException("Invalid SegmentAttribute Enumeration");
      }
   }
   
   public static void validateSpeedLimitType(String str) {
      String myString = "unknown maxSpeedInSchoolZone maxSpeedInSchoolZoneWhenChildrenArePresent maxSpeedInConstructionZone vehicleMinSpeed vehicleMaxSpeed vehicleNightMaxSpeed truckMinSpeed truckMaxSpeed truckNightMaxSpeed vehiclesWithTrailerMinSpeed vehiclesWithTrailersMaxSpeed vehiclesWithTrailersNightMaxSpeed";
      CharSequence cs = str;
      if (myString.contains(cs)) {
         return;
      } else {
         throw new IllegalArgumentException("Invalid SpeedLimitAttribute Enumeration");
      }
   }
   
   public static void validateVelocity(int vel) {
      if (vel < 0 || vel > 8191)
         throw new IllegalArgumentException("Invalid Velocity");
   }
   
   public static void validateDeltaAngle(int d) {
      if (d < -150 || d > 150)
         throw new IllegalArgumentException("Invalid Delta Angle");
   }
   
   public static void validateCrownPoint(int c) {
      if (c < -128 || c > 127)
         throw new IllegalArgumentException("Invalid Crown Point");
   }
   
   public static void validateLaneAngle(int a) {
      if (a < -180 || a > 180)
         throw new IllegalArgumentException("Invalid LaneAngle");
   }
}
