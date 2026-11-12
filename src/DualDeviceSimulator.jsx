import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { RefreshCw, Image as ImageIcon, Send, Wifi, Mic, Bookmark, Trash, UserPlus, CheckCircle2 } from 'lucide-react';

export default function DualDeviceSimulator() {
  const [step, setStep] = useState(0);

  useEffect(() => {
    const timer = setInterval(() => {
      setStep((prev) => (prev + 1) % 15);
    }, 4000);
    return () => clearInterval(timer);
  }, []);

  const DeviceMockup = ({ title, isPaired, isRight, children }) => (
    <div style={{
      width: '280px',
      height: '500px',
      background: '#000',
      borderRadius: '30px',
      border: '8px solid #2a2a35',
      position: 'relative',
      overflow: 'hidden',
      display: 'flex',
      flexDirection: 'column',
      boxShadow: '0 20px 40px rgba(0,0,0,0.5)'
    }}>
      {/* Notch */}
      <div style={{ position: 'absolute', top: 0, left: '50%', transform: 'translateX(-50%)', width: '120px', height: '20px', background: '#2a2a35', borderBottomLeftRadius: '10px', borderBottomRightRadius: '10px', zIndex: 10 }} />
      
      {/* Header */}
      <div style={{ background: '#1c1d25', padding: '25px 15px 10px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', borderBottom: '1px solid rgba(255,255,255,0.1)' }}>
        <span style={{ fontWeight: 'bold' }}>{title}</span>
        {isPaired ? <Wifi size={16} color="var(--accent-neon)" /> : <RefreshCw size={16} color="gray" className={step === 0 ? "spinning" : ""} />}
      </div>

      {/* Screen Area */}
      <div style={{ flex: 1, padding: '15px', background: '#0f1015', display: 'flex', flexDirection: 'column', gap: '10px', overflowY: 'auto' }}>
        {children}
      </div>

      {/* Input Area (Only visible when paired) */}
      {isPaired && (
        <div style={{ padding: '15px', background: '#1c1d25', display: 'flex', gap: '10px', borderTop: '1px solid rgba(255,255,255,0.1)', alignItems: 'center' }}>
          <div style={{ flex: 1, background: '#2a2a35', borderRadius: '20px', padding: '8px 15px', color: 'gray', fontSize: '0.9rem' }}>
            {(step === 3 && !isRight) ? "Typing..." : "Message"}
          </div>
          {(step >= 8 && step <= 9 && isRight) ? (
            <Mic size={20} color="var(--accent-neon)" />
          ) : (
            <Send size={20} color={(step === 3 && !isRight) ? "var(--accent-purple)" : "gray"} />
          )}
        </div>
      )}
    </div>
  );

  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', width: '100%', margin: '60px 0' }}>
      
      <style>{`
        @keyframes spin { 100% { transform: rotate(360deg); } }
        .spinning { animation: spin 2s linear infinite; }
      `}</style>

      <h3 style={{ marginBottom: '40px', fontSize: '1.5rem', textAlign: 'center', color: 'var(--accent-neon)', minHeight: '60px' }}>
        {step === 0 && "1. Scanning for nearby offline peers..."}
        {step === 1 && "2. Found a peer! Initiating secure handshake..."}
        {step === 2 && "3. Devices paired successfully over Mesh Network."}
        {step === 3 && "4. Typing an offline text message..."}
        {step === 4 && "5. Transmitting text via Bluetooth/Wi-Fi Direct..."}
        {step === 5 && "6. Message received."}
        {step === 6 && "7. Sending a high-resolution photo..."}
        {step === 7 && "8. Photo securely received."}
        {step === 8 && "9. Partner records a Voice Note..."}
        {step === 9 && "10. Transmitting Voice Note audio..."}
        {step === 10 && "11. Voice Note received!"}
        {step === 11 && "12. Saving the important photo locally..."}
        {step === 12 && "13. Partner realizes Voice Note was a mistake..."}
        {step === 13 && "14. Initiating 'Delete for Everyone' protocol..."}
        {step === 14 && "15. Message completely scrubbed from both devices."}
      </h3>

      <div style={{ display: 'flex', gap: '60px', position: 'relative' }}>
        
        {/* DEVICE A (You) */}
        <DeviceMockup title="You" isPaired={step >= 2} isRight={false}>
          {step < 2 ? (
            <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: '20px' }}>
              <motion.div animate={{ scale: [1, 1.2, 1] }} transition={{ repeat: Infinity, duration: 1.5 }}>
                <Wifi size={48} color={step === 1 ? "var(--accent-neon)" : "gray"} />
              </motion.div>
              <div style={{ color: 'gray' }}>{step === 0 ? "Discovering..." : "Connecting to Partner..."}</div>
            </div>
          ) : (
            <AnimatePresence>
              {/* Text Message */}
              {(step >= 4) && (
                <motion.div 
                  initial={{ opacity: 0, x: 20 }} animate={{ opacity: 1, x: 0 }}
                  style={{ background: 'var(--accent-purple)', padding: '10px 15px', borderRadius: '15px', borderBottomRightRadius: '0', alignSelf: 'flex-end', maxWidth: '80%' }}
                >
                  We need to move to higher ground.
                </motion.div>
              )}
              
              {/* Image Message */}
              {(step >= 6) && (
                <motion.div 
                  initial={{ opacity: 0, scale: 0.8 }} animate={{ opacity: 1, scale: 1 }}
                  style={{ position: 'relative', background: 'var(--accent-purple)', padding: '5px', borderRadius: '15px', borderBottomRightRadius: '0', alignSelf: 'flex-end', maxWidth: '80%', marginTop: '10px' }}
                >
                  <div style={{ width: '150px', height: '100px', background: '#3a3a45', borderRadius: '10px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <ImageIcon size={32} color="rgba(255,255,255,0.5)" />
                  </div>
                  {/* Save Indicator */}
                  {step >= 11 && (
                    <motion.div initial={{ scale: 0 }} animate={{ scale: 1 }} style={{ position: 'absolute', top: '-10px', right: '-10px', background: 'var(--accent-neon)', borderRadius: '50%', padding: '5px' }}>
                      <Bookmark size={14} color="#000" />
                    </motion.div>
                  )}
                </motion.div>
              )}

              {/* Voice Note from Partner */}
              {(step >= 10 && step < 14) && (
                <motion.div 
                  initial={{ opacity: 0, x: -20 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, height: 0, marginTop: 0, padding: 0 }}
                  style={{ background: '#2a2a35', padding: '10px 15px', borderRadius: '15px', borderBottomLeftRadius: '0', alignSelf: 'flex-start', width: '180px', marginTop: '10px', display: 'flex', alignItems: 'center', gap: '10px', overflow: 'hidden' }}
                >
                  <div style={{ background: 'var(--accent-neon)', borderRadius: '50%', padding: '8px' }}><Mic size={16} color="#000" /></div>
                  <div style={{ height: '4px', background: 'gray', flex: 1, borderRadius: '2px' }} />
                  <span style={{ fontSize: '0.8rem', color: 'gray' }}>0:12</span>
                </motion.div>
              )}
            </AnimatePresence>
          )}
        </DeviceMockup>


        {/* FLYING PACKETS / VISUALIZATIONS */}
        <div style={{ position: 'absolute', top: '50%', left: '50%', transform: 'translate(-50%, -50%)', width: '60px', height: '100%' }}>
          
          {/* Pairing Request */}
          {step === 1 && (
             <motion.div 
             initial={{ x: -140, opacity: 1 }} animate={{ x: 140, opacity: 0 }} transition={{ duration: 1.5, ease: "linear" }}
             style={{ position: 'absolute', top: '40%', width: '25px', height: '25px', background: 'transparent', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--accent-neon)' }}
           >
             <UserPlus size={24} />
           </motion.div>
          )}

          {/* Text Message */}
          {step === 4 && (
            <motion.div 
              initial={{ x: -140, y: -90, opacity: 1 }} animate={{ x: 140, y: -90, opacity: 0 }} transition={{ duration: 1.5, ease: "easeOut" }}
              style={{ position: 'absolute', width: '15px', height: '15px', background: 'var(--accent-purple)', borderRadius: '50%', boxShadow: '0 0 10px var(--accent-purple)' }}
            />
          )}

          {/* Image Packet */}
          {step === 6 && (
            <motion.div 
              initial={{ x: -140, y: -10, opacity: 1 }} animate={{ x: 140, y: -10, opacity: 0 }} transition={{ duration: 1.5, ease: "easeOut" }}
              style={{ position: 'absolute', width: '25px', height: '25px', background: 'var(--accent-purple)', borderRadius: '5px', boxShadow: '0 0 15px var(--accent-purple)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
            >
              <ImageIcon size={14} color="white" />
            </motion.div>
          )}

          {/* Voice Note Packet (Moving Right to Left) */}
          {step === 9 && (
            <motion.div 
              initial={{ x: 140, y: 70, opacity: 1 }} animate={{ x: -140, y: 70, opacity: 0 }} transition={{ duration: 1.5, ease: "easeOut" }}
              style={{ position: 'absolute', width: '25px', height: '25px', background: 'var(--accent-neon)', borderRadius: '50%', boxShadow: '0 0 15px var(--accent-neon)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
            >
              <Mic size={14} color="#000" />
            </motion.div>
          )}

          {/* Delete Packet (Right to Left) */}
          {step === 13 && (
            <motion.div 
              initial={{ x: 140, y: 70, opacity: 1 }} animate={{ x: -140, y: 70, opacity: 0 }} transition={{ duration: 1, ease: "linear" }}
              style={{ position: 'absolute', width: '20px', height: '20px', background: 'red', borderRadius: '50%', boxShadow: '0 0 15px red', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
            >
              <Trash size={12} color="white" />
            </motion.div>
          )}
        </div>


        {/* DEVICE B (Partner) */}
        <DeviceMockup title="Partner" isPaired={step >= 2} isRight={true}>
          {step < 2 ? (
            <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: '20px' }}>
              <motion.div animate={{ scale: [1, 1.2, 1] }} transition={{ repeat: Infinity, duration: 1.5, delay: 0.5 }}>
                <Wifi size={48} color={step === 1 ? "var(--accent-neon)" : "gray"} />
              </motion.div>
              <div style={{ color: 'gray' }}>{step === 0 ? "Searching..." : "Receiving Handshake..."}</div>
            </div>
          ) : (
            <AnimatePresence>
              {/* Received Text */}
              {(step >= 5) && (
                <motion.div 
                  initial={{ opacity: 0, x: -20 }} animate={{ opacity: 1, x: 0 }}
                  style={{ background: '#2a2a35', padding: '10px 15px', borderRadius: '15px', borderBottomLeftRadius: '0', alignSelf: 'flex-start', maxWidth: '80%' }}
                >
                  We need to move to higher ground.
                </motion.div>
              )}
              
              {/* Received Image */}
              {(step >= 7) && (
                <motion.div 
                  initial={{ opacity: 0, scale: 0.8 }} animate={{ opacity: 1, scale: 1 }}
                  style={{ background: '#2a2a35', padding: '5px', borderRadius: '15px', borderBottomLeftRadius: '0', alignSelf: 'flex-start', maxWidth: '80%', marginTop: '10px' }}
                >
                  <div style={{ width: '150px', height: '100px', background: '#3a3a45', borderRadius: '10px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <ImageIcon size={32} color="rgba(255,255,255,0.5)" />
                  </div>
                </motion.div>
              )}

              {/* Sent Voice Note */}
              {(step >= 9 && step < 14) && (
                <motion.div 
                  initial={{ opacity: 0, x: 20 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, height: 0, marginTop: 0, padding: 0 }}
                  style={{ background: 'var(--accent-neon)', padding: '10px 15px', borderRadius: '15px', borderBottomRightRadius: '0', alignSelf: 'flex-end', width: '180px', marginTop: '10px', display: 'flex', alignItems: 'center', gap: '10px', overflow: 'hidden' }}
                >
                  <div style={{ background: '#1c1d25', borderRadius: '50%', padding: '8px' }}><Mic size={16} color="var(--accent-neon)" /></div>
                  <div style={{ height: '4px', background: '#1c1d25', flex: 1, borderRadius: '2px' }} />
                  <span style={{ fontSize: '0.8rem', color: '#1c1d25', fontWeight: 'bold' }}>0:12</span>
                </motion.div>
              )}
            </AnimatePresence>
          )}
        </DeviceMockup>

      </div>

      <button 
        onClick={() => setStep(0)} 
        style={{ marginTop: '30px', background: 'transparent', border: '1px solid var(--accent-neon)', color: 'var(--accent-neon)', padding: '8px 16px', borderRadius: '5px', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '5px' }}
      >
        <RefreshCw size={16} /> Replay App Simulation
      </button>

    </div>
  );
}
