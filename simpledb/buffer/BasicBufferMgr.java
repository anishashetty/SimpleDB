package simpledb.buffer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import simpledb.file.*;


/**
 * Manages the pinning and unpinning of buffers to blocks.
 * @author Edward Sciore
 *
 */
class BasicBufferMgr {
   private Buffer[] bufferpool;
   private int numAvailable;
   private HashMap<Block, Buffer> bufferPoolMap;
   
   /**
    * Creates a buffer manager having the specified number 
    * of buffer slots.
    * This constructor depends on both the {@link FileMgr} and
    * {@link simpledb.log.LogMgr LogMgr} objects 
    * that it gets from the class
    * {@link simpledb.server.SimpleDB}.
    * Those objects are created during system initialization.
    * Thus this constructor cannot be called until 
    * {@link simpledb.server.SimpleDB#initFileAndLogMgr(String)} or
    * is called first.
    * @param numbuffs the number of buffer slots to allocate
    */
   public BasicBufferMgr(int numbuffs) {
	   
	   bufferPoolMap= new HashMap<Block, Buffer>(numbuffs);
	   numAvailable = numbuffs;
		
	   bufferpool = new Buffer[numbuffs];	
	   
		/*for (int i=0; i<numbuffs; i++)
		{   
			bufferPoolMap.put(null,new Buffer());
		}*/
   }
   
   /**
    * Flushes the dirty buffers modified by the specified transaction.
    * @param txnum the transaction's id number
    */
   synchronized void flushAll(int txnum) {
	   Collection<Buffer> Mapbufferpool =  bufferPoolMap.values();
	   Iterator<Buffer> itr = Mapbufferpool.iterator();
	  
	   Buffer buff=new Buffer();
	   while (itr.hasNext())
	   {
	      buff=(Buffer)itr.next();
		  if (buff!=null && buff.isModifiedBy(txnum))
	         buff.flush();		        
	   }
   }
   
   /**
    * Pins a buffer to the specified block. 
    * If there is already a buffer assigned to that block
    * then that buffer is used;  
    * otherwise, an unpinned buffer from the pool is chosen.
    * Returns a null value if there are no available buffers.
    * @param blk a reference to a disk block
    * @return the pinned buffer
    */
   synchronized Buffer pin(Block blk) {
      Buffer buff = findExistingBuffer(blk);
      if (buff == null) {
         buff = chooseUnpinnedBuffer();
         if (buff == null)
            return null;
         buff.assignToBlock(blk);
      }
      if (!buff.isPinned())
         numAvailable--;
      buff.pin();
      return buff;
   }
   
   /**
    * Allocates a new block in the specified file, and
    * pins a buffer to it. 
    * Returns null (without allocating the block) if 
    * there are no available buffers.
    * @param filename the name of the file
    * @param fmtr a pageformatter object, used to format the new block
    * @return the pinned buffer
    */
   synchronized Buffer pinNew(String filename, PageFormatter fmtr) {
      Buffer buff = chooseUnpinnedBuffer();
      if (buff == null)
         return null;
      buff.assignToNew(filename, fmtr);
      numAvailable--;
      buff.pin();
      return buff;
   }
   
   /**
    * Unpins the specified buffer.
    * @param buff the buffer to be unpinned
    */
   synchronized void unpin(Buffer buff) {
      buff.unpin();
      if (!buff.isPinned())
         numAvailable++;
   }
   
   /**
    * Returns the number of available (i.e. unpinned) buffers.
    * @return the number of available buffers
    */
   int available() {
      return numAvailable;
   }
   
   private Buffer findExistingBuffer(Block blk) {
     
	  /* for (Buffer buff : bufferpool) {
         Block b = buff.block();
         if (b != null && b.equals(blk))
            return buff;
      }
      return null; */
	   //author: Anisha changing to use Map 
	  return bufferPoolMap.get(blk);
	   
	   
   }
   
   private Buffer chooseUnpinnedBuffer() {
	   Buffer buff=new Buffer();
	   // Case 1:
	   // Check if Map size is zero. In this case return a new Buff element
	   
	   if(bufferPoolMap.size()==0)
	   {
		   bufferPoolMap.put(buff.block(),buff);
		   return buff;
	   }
	   else
	   {
	   //Case 2:
	   //Check if there exists an unpinned buff in current bufferPoolMap
	   Collection<Buffer> Mapbufferpool =  bufferPoolMap.values();
	   Iterator<Buffer> itr = Mapbufferpool.iterator();
	   while (itr.hasNext())
	   {
	     buff=(Buffer) itr.next();
		 if (!buff.isPinned() && buff !=null)
         return buff;
	   }
	   
	   //Case 3:
	   //There are no unpinned buffers. Now check to see if the capacity<numAvailable
	   if(bufferPoolMap.size()< numAvailable)
	   {
		   buff=new Buffer();
		   bufferPoolMap.put(buff.block(),buff);
		   return buff;
	   }
	   
	   }
      return null; 
	   
	   
   }
}
