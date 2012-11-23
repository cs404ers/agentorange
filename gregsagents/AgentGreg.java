import se.sics.tasim.aw.Agent;
import se.sics.tasim.aw.Message;
import se.sics.tasim.props.StartInfo;
import se.sics.tasim.props.SimulationStatus;
import se.sics.isl.transport.Transportable;
import edu.umich.eecs.tac.props.*;

import java.util.*;
import java.io.*;
import java.net.*;


public class AgentGreg extends Agent{
    /**
     * Basic simulation information. {@link StartInfo} contains
     * <ul>
     * <li>simulation ID</li>
     * <li>simulation start time</li>
     * <li>simulation length in simulation days</li>
     * <li>actual seconds per simulation day</li>
     * </ul>
     * An agent should receive the {@link StartInfo} at the beginning of the game or during recovery.
     */
    private StartInfo startInfo;
    /**
     * Basic auction slot information. {@link SlotInfo} contains
     * <ul>
     * <li>the number of regular slots</li>
     * <li>the number of promoted slots</li>
     * <li>promoted slot bonus</li>
     * </ul>
     * An agent should receive the {@link SlotInfo} at the beginning of the game or during recovery.
     * This information is identical for all auctions over all query classes.
     */
    protected SlotInfo slotInfo;
    /**
     * The retail catalog. {@link RetailCatalog} contains
     * <ul>
     * <li>the product set</li>
     * <li>the sales profit per product</li>
     * <li>the manufacturer set</li>
     * <li>the component set</li>
     * </ul>
     * An agent should receive the {@link RetailCatalog} at the beginning of the game or during recovery.
     */
    protected RetailCatalog retailCatalog;
    /**
     * The basic advertiser specific information. {@link AdvertiserInfo} contains
     * <ul>
     * <li>the manufacturer specialty</li>
     * <li>the component specialty</li>
     * <li>the manufacturer bonus</li>
     * <li>the component bonus</li>
     * <li>the distribution capacity discounter</li>
     * <li>the address of the publisher agent</li>
     * <li>the distribution capacity</li>
     * <li>the address of the advertiser agent</li>
     * <li>the distribution window</li>
     * <li>the target effect</li>
     * <li>the focus effects</li>
     * </ul>
     * An agent should receive the {@link AdvertiserInfo} at the beginning of the game or during recovery.
     */
    protected AdvertiserInfo advertiserInfo;

    /**
     * The basic publisher information. {@link PublisherInfo} contains
     * <ul>
     * <li>the squashing parameter</li>
     * </ul>
     * An agent should receive the {@link PublisherInfo} at the beginning of the game or during recovery.
     */
    protected PublisherInfo publisherInfo;

    /**
     * The list contains all of the {@link SalesReport sales report} delivered to the agent.  Each
     * {@link SalesReport sales report} contains the conversions and sales revenue accrued by the agent for each query
     * class during the period.
     */
    protected Queue<SalesReport> salesReports;

    /**
     * The list contains all of the {@link QueryReport query reports} delivered to the agent.  Each
     * {@link QueryReport query report} contains the impressions, clicks, cost, average position, and ad displayed
     * by the agent for each query class during the period as well as the positions and displayed ads of all advertisers
     * during the period for each query class.
     */
    protected Queue<QueryReport> queryReports;

    /**
     * List of all the possible queries made available in the {@link RetailCatalog retail catalog}.
     */
    protected Set<Query> querySpace;
    
    protected Socket comms;
    
    private Query qPA = new Query("pg","audio");
    private double aPA = 0.5;
    
    private Query qPD = new Query("pg", "dvd");
    private double aPD = 0.5;
    
    private Query qPT = new Query("pg","tv");
    private double aPT = 0.5;
    
    private Query qFA = new Query("flat","audio");
    private double aFA = 0.5;
    
    private Query qFD = new Query("flat", "dvd");
    private double aFD = 0.5;
    
    private Query qFT = new Query("flat","tv");
    private double aFT = 0.5;
    
    private Query qLA = new Query("lioneer","audio");
    private double aLA = 0.5;
    
    private Query qLD = new Query("lioneer", "dvd");
    private double aLD = 0.5;
    
    private Query qLT = new Query("lioneer","tv");
    private double aLT = 0.5;
    
    private double up = 0.01;
    private double down = -0.01;
    
    public AgentGreg() {
        salesReports = new LinkedList<SalesReport>();
        queryReports = new LinkedList<QueryReport>();
        querySpace = new LinkedHashSet<Query>();
    }

	 protected void messageReceived(Message message) {
	        Transportable content = message.getContent();

	        if (content instanceof QueryReport) {
	            handleQueryReport((QueryReport) content);
	        } else if (content instanceof SalesReport) {
	            handleSalesReport((SalesReport) content);
	        } else if (content instanceof SimulationStatus) {
	            handleSimulationStatus((SimulationStatus) content);
	        } else if (content instanceof PublisherInfo) {
	            handlePublisherInfo((PublisherInfo) content);
	        } else if (content instanceof SlotInfo) {
	            handleSlotInfo((SlotInfo) content);
	        } else if (content instanceof RetailCatalog) {
	            handleRetailCatalog((RetailCatalog) content);
	        } else if (content instanceof AdvertiserInfo) {
	            handleAdvertiserInfo((AdvertiserInfo) content);
	        } else if (content instanceof StartInfo) {
	            handleStartInfo((StartInfo) content);
	        }
	        
	        System.out.println("(AgentGreg): Message Received");
	        
	    }
      
      private double change(double x, double deltaX)
        {
          x = x + deltaX;
          if (x < 0.05)
            {
              x = 0.05;
            }
          else if (x > 0.95) 
            {
              x = 0.95;
            }
          return x;  
        }

      private double getVPC(Query query)
        {
          return (((SalesReport)((LinkedList)salesReports).getLast()).getRevenue(query))/(((QueryReport)(((LinkedList)queryReports).getLast())).getCost(query));
        }

      private double getRevMinCost(Query query)
        {
          return ((SalesReport)((LinkedList)salesReports).getLast()).getRevenue(query) - ((QueryReport)(((LinkedList)queryReports).getLast())).getCost(query);
        }

      private double getCPC(Query query)
        {
          return ((QueryReport)(((LinkedList)queryReports).getLast())).getCPC(query);
        }

	    /**
	     * Sends a constructed {@link BidBundle} from any updated bids, ads, or spend limits.
	     */


	    protected void sendBidAndAds() {
	        BidBundle bidBundle = new BidBundle();

	        String publisherAddress = advertiserInfo.getPublisherId();
          
          for(Query query : querySpace) {
	            // The publisher will interpret a NaN bid as
	            // a request to persist the prior day's bid
	            double bid = 1;
	            // bid = [ calculated optimal bid ]

              if (query.equals(qPA))
                {
                  if (getRevMinCost(query) > 0)
                    {
                      aPA = change(aPA,up);
                    }
                  else
                    {
                      aPA = change(aPA,down);
                    }
                  bid = aPA*getVPC(query);
                  System.out.println("Bid pa: "+aPA+" "+bid);
                }
              else if (query.equals(qPD))
               {
                  if (getRevMinCost(query) > 0)
                    {
                      aPD = change(aPD,up);
                    }
                  else
                    {
                      aPD = change(aPD,down);
                    }
                  bid = aPD*getVPC(query);
                  System.out.println("Bid pd: "+aPD+" "+bid);
                }
              else if (query.equals(qPT))
                {
                  if (getRevMinCost(query) > 0)
                    {
                      aPT = change(aPT,up);
                    }
                  else
                    {
                      aPT = change(aPT,down);
                    }
                  bid = aPT*getVPC(query);
                  System.out.println("Bid pt: "+aPT+" "+bid);
                }
              else if (query.equals(qFA))
                {
                  if (getRevMinCost(query) > 0)
                    {
                      aFA = change(aFA,up);
                    }
                  else
                    {
                      aFA = change(aFA,down);
                    }
                  bid = aFA*getVPC(query);
                  System.out.println("Bid fa: "+aFA+" "+bid);
                }
              else if (query.equals(qFD))
                {
                  if (getRevMinCost(query) > 0)
                    {
                      aFD = change(aFD,up);
                    }
                  else
                    {
                      aFD = change(aFD,down);
                    }
                  bid = aFD*getVPC(query);
                  System.out.println("Bid fd: "+aFD+" "+bid);
                }
              else if (query.equals(qFT))
                {
                  if (getRevMinCost(query) > 0)
                    {
                      aFT = change(aFT,up);
                    }
                  else
                    {
                      aFT = change(aFT,down);
                    }
                  bid = aFT*getVPC(query);
                  System.out.println("Bid ft: "+aFT+" "+bid);
                }              
              else if (query.equals(qLA))
                {
                  if (getRevMinCost(query) > 0)
                    {
                      aLA = change(aLA,up);
                    }
                  else
                    {
                      aLA = change(aLA,down);
                    }
                  bid = aLA*getVPC(query);
                  System.out.println("Bid la: "+aLA+" "+bid);
                }
              else if (query.equals(qLD))
                {
                  if (getRevMinCost(query) > 0)
                    {
                      aLD = change(aLD,up);
                    }
                  else
                    {
                      aLD = change(aLD,down);
                    }
                  bid = aLD*getVPC(query);
                  System.out.println("Bid ld: "+aLD+" "+bid);
                }
              else if (query.equals(qLT))
                {
                  if (getRevMinCost(query) > 0)
                    {
                      aLT = change(aLT,up);
                    }
                  else
                    {
                      aLT = change(aLT,down);
                    }
                  bid = aLT*getVPC(query);
                  System.out.println("Bid lt: "+aLT+" "+bid);
                }          
              Double bidCheck = new Double(bid);
              if (bidCheck.isNaN()) 
                {
                  //System.out.println("FIRST BID");
                  bid = 1;
                }
              
             // The publisher will interpret a null ad as
	            // a request to persist the prior day's ad
	            Ad ad = new Ad();
	            
	            if ((query.getType() == QueryType.FOCUS_LEVEL_ZERO))
                {
                  ad = new Ad();
                }
              else if ((query.getType() == QueryType.FOCUS_LEVEL_ONE))
                {
                  if (query.getManufacturer() == null)
                    {
                      Product p = new Product(advertiserInfo.getManufacturerSpecialty(),query.getComponent());
                      ad = new Ad(p);
                    }
                  else
                    {
                      Product p = new Product(query.getManufacturer(),advertiserInfo.getComponentSpecialty());
                      ad = new Ad(p);
                    }
                }
              else
                {
                  Product p = new Product(query.getManufacturer(),query.getComponent());
                  ad = new Ad(p);
                }
 
	            // The publisher will interpret a NaN spend limit as
	            // a request to persist the prior day's spend limit
	            double spendLimit = 100;
	            // spendLimit = [ calculated optimal spend limit ]


	            // Set the daily updates to the ad campaigns for this query class
	            bidBundle.addQuery(query,  bid, ad);
	            bidBundle.setDailyLimit(query, spendLimit);
	        }

	        // The publisher will interpret a NaN campaign spend limit as
	        // a request to persist the prior day's campaign spend limit
	        double campaignSpendLimit = 10000;
	        // campaignSpendLimit = [ calculated optimal campaign spend limit ]


	        // Set the daily updates to the campaign spend limit
	        bidBundle.setCampaignDailySpendLimit(campaignSpendLimit);

	        // Send the bid bundle to the publisher
	        if (publisherAddress != null) {
	            sendMessage(publisherAddress, bidBundle);
	        }
	    }


	    /**
	     * Processes an incoming query report.
	     *
	     * @param queryReport the daily query report.
	     */
	    protected void handleQueryReport(QueryReport queryReport) {
	        queryReports.add(queryReport);
	    }

	    /**
	     * Processes an incoming sales report.
	     *
	     * @param salesReport the daily sales report.
	     */
	    protected void handleSalesReport(SalesReport salesReport) {
	        salesReports.add(salesReport);
	    }

	    /**
	     * Processes a simulation status notification.  Each simulation day the {@link SimulationStatus simulation status }
	     * notification is sent after the other daily messages ({@link QueryReport} {@link SalesReport} have been sent.
	     *
	     * @param simulationStatus the daily simulation status.
	     */
	    protected void handleSimulationStatus(SimulationStatus simulationStatus) {
	        sendBidAndAds();
	    }

	    /**
	     * Processes the publisher information.
	     * @param publisherInfo the publisher information.
	     */
	    protected void handlePublisherInfo(PublisherInfo publisherInfo) {
	        this.publisherInfo = publisherInfo;
	    }

	    /**
	     * Processrs the slot information.
	     * @param slotInfo the slot information.
	     */
	    protected void handleSlotInfo(SlotInfo slotInfo) {
	        this.slotInfo = slotInfo;
	    }

	    /**
	     * Processes the retail catalog.
	     * @param retailCatalog the retail catalog.
	     */
	    protected void handleRetailCatalog(RetailCatalog retailCatalog) {
	        this.retailCatalog = retailCatalog;

	        // The query space is all the F0, F1, and F2 queries for each product
	        // The F0 query class
	        if(retailCatalog.size() > 0) {
	            querySpace.add(new Query(null, null));
	        }

	        for(Product product : retailCatalog) {
	            // The F1 query classes
	            // F1 Manufacturer only
	            querySpace.add(new Query(product.getManufacturer(), null));
	            // F1 Component only
	            querySpace.add(new Query(null, product.getComponent()));

	            // The F2 query class
	            querySpace.add(new Query(product.getManufacturer(), product.getComponent()));
	        }
	    }

	    /**
	     * Processes the advertiser information.
	     * @param advertiserInfo the advertiser information.
	     */
	    protected void handleAdvertiserInfo(AdvertiserInfo advertiserInfo) {
	        this.advertiserInfo = advertiserInfo;
	    }

	    /**
	     * Processes the start information.
	     * @param startInfo the start information.
	     */
	    protected void handleStartInfo(StartInfo startInfo) {
	        this.startInfo = startInfo;
	    }

	    /**
	     * Prepares the agent for a new simulation.
	     */
	    protected void simulationSetup() {
	    	
	    	try {
	    	
	    		comms = new Socket("localhost", 6502);
	    	
	    	} catch (IOException e) {
	    		
	    		
	    	}
	    }

	    /**
	     * Runs any post-processes required for the agent after a simulation ends.
	     */
	    protected void simulationFinished() {
	        salesReports.clear();
	        queryReports.clear();
	        querySpace.clear();
	    }
	}

