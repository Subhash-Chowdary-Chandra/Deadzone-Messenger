import React, { useRef, useState } from 'react';
import { motion, useScroll } from 'framer-motion';
import { 
  WifiOff, Radio, Shield, Image as ImageIcon, Bookmark, ArrowDownCircle, 
  Activity, Database, Network, Code, Lock, Send, Download, Mail, 
  ExternalLink, CheckCircle, Smartphone, MapPin, Users, Sparkles, ChevronRight 
} from 'lucide-react';
import MeshSimulator from './MeshSimulator';
import DualDeviceSimulator from './DualDeviceSimulator';
import OfflineSimulator from './OfflineSimulator';

function App() {
  const containerRef = useRef(null);
  const [emailInput, setEmailInput] = useState('');
  const [submitted, setSubmitted] = useState(false);

  const handleSubmit = (e) => {
    e.preventDefault();
    if (emailInput.trim()) {
      setSubmitted(true);
      window.location.href = `mailto:subhashchowdarychandra@gmail.com?subject=Deadzone%20Messenger%20Early%20Access&body=Hi%20Subhash,%20I%20would%20like%20to%20request%20early%20access%20to%20Deadzone%20Messenger.%20My%20email:%20${encodeURIComponent(emailInput)}`;
    }
  };

  return (
    <div ref={containerRef} style={{ background: 'var(--bg-dark)', color: 'var(--text-primary)', minHeight: '100vh', scrollBehavior: 'smooth' }}>
      
      {/* NAVIGATION BAR */}
      <nav style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        height: '70px',
        background: 'rgba(15, 16, 21, 0.85)',
        backdropFilter: 'blur(16px)',
        WebkitBackdropFilter: 'blur(16px)',
        borderBottom: '1px solid rgba(255, 255, 255, 0.08)',
        zIndex: 1000,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '0 30px'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <div style={{ 
            width: '38px', 
            height: '38px', 
            borderRadius: '10px', 
            background: 'linear-gradient(135deg, rgba(0, 240, 255, 0.2), rgba(176, 92, 255, 0.2))',
            display: 'flex', 
            alignItems: 'center', 
            justifyContent: 'center',
            border: '1px solid rgba(0, 240, 255, 0.4)'
          }}>
            <Radio size={20} color="var(--accent-neon)" />
          </div>
          <div>
            <span style={{ fontWeight: '800', letterSpacing: '1px', fontSize: '1.1rem' }}>DEADZONE</span>
            <span style={{ color: 'var(--accent-neon)', marginLeft: '6px', fontSize: '0.8rem', fontWeight: '600', padding: '2px 8px', borderRadius: '12px', background: 'rgba(0, 240, 255, 0.1)', border: '1px solid rgba(0, 240, 255, 0.3)' }}>BETA</span>
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '25px' }}>
          <a href="#how-it-works" style={{ color: 'var(--text-secondary)', textDecoration: 'none', fontSize: '0.95rem', transition: 'color 0.2s' }}>How It Works</a>
          <a href="#demo" style={{ color: 'var(--text-secondary)', textDecoration: 'none', fontSize: '0.95rem', transition: 'color 0.2s' }}>Live Demo</a>
          <a href="#tech" style={{ color: 'var(--text-secondary)', textDecoration: 'none', fontSize: '0.95rem', transition: 'color 0.2s' }}>Tech Stack</a>
          <a href="#contact" style={{
            background: 'linear-gradient(135deg, var(--accent-neon), var(--accent-purple))',
            color: '#0f1015',
            padding: '8px 18px',
            borderRadius: '20px',
            fontWeight: '700',
            fontSize: '0.9rem',
            textDecoration: 'none',
            display: 'flex',
            alignItems: 'center',
            gap: '6px',
            boxShadow: '0 0 15px rgba(0, 240, 255, 0.3)'
          }}>
            <Download size={15} /> Get Access
          </a>
        </div>
      </nav>

      {/* SECTION 1: HERO (Unique Value Proposition Upfront) */}
      <section style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', justifyContent: 'center', alignItems: 'center', position: 'relative', overflow: 'hidden', padding: '140px 20px 80px' }}>
        <div className="radar-ring" style={{ animationDelay: '0s' }}></div>
        <div className="radar-ring" style={{ animationDelay: '1s' }}></div>
        <div className="radar-ring" style={{ animationDelay: '2s' }}></div>
        
        <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'center', gap: '60px', zIndex: 10, maxWidth: '1200px', margin: '0 auto' }}>
          
          <motion.div
            initial={{ opacity: 0, x: -50 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 1 }}
            style={{ flex: '1 1 500px', textAlign: 'left' }}
          >
            <div style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '8px',
              padding: '6px 14px',
              borderRadius: '20px',
              background: 'rgba(0, 240, 255, 0.1)',
              border: '1px solid rgba(0, 240, 255, 0.3)',
              color: 'var(--accent-neon)',
              fontSize: '0.85rem',
              fontWeight: '600',
              marginBottom: '20px'
            }}>
              <Sparkles size={14} /> DECENTRALIZED OFF-GRID P2P MESSAGING
            </div>

            <h1 style={{ fontSize: '3.6rem', fontWeight: '900', lineHeight: '1.1', marginBottom: '15px' }}>
              No Signal.<br />
              <span style={{ 
                background: 'linear-gradient(135deg, var(--accent-neon), var(--accent-purple))', 
                WebkitBackgroundClip: 'text', 
                WebkitTextFillColor: 'transparent' 
              }}>
                Never Isolated.
              </span>
            </h1>

            <p style={{ fontSize: '1.25rem', color: 'var(--text-secondary)', lineHeight: '1.7', marginBottom: '35px' }}>
              Stay seamlessly connected when cellular towers fail and Wi-Fi drops. Deadzone Messenger turns nearby smartphones into an encrypted, self-healing peer-to-peer mesh network—<strong>100% free, private, and serverless.</strong>
            </p>

            {/* CALLS TO ACTION (CTAs) */}
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '16px', alignItems: 'center' }}>
              <a 
                href="#demo"
                style={{
                  background: 'linear-gradient(135deg, var(--accent-neon), #00c8d7)',
                  color: '#0f1015',
                  padding: '14px 28px',
                  borderRadius: '12px',
                  fontWeight: '700',
                  fontSize: '1.05rem',
                  textDecoration: 'none',
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '8px',
                  boxShadow: '0 4px 20px rgba(0, 240, 255, 0.35)',
                  transition: 'transform 0.2s'
                }}
              >
                Experience Live Demo <ChevronRight size={18} />
              </a>

              <a 
                href="#contact"
                style={{
                  background: 'rgba(28, 29, 37, 0.8)',
                  color: 'var(--text-primary)',
                  padding: '14px 24px',
                  borderRadius: '12px',
                  fontWeight: '600',
                  fontSize: '1.05rem',
                  textDecoration: 'none',
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '8px',
                  border: '1px solid rgba(255, 255, 255, 0.15)',
                  transition: 'background 0.2s'
                }}
              >
                <Download size={18} color="var(--accent-purple)" /> Get Early Access APK
              </a>
            </div>

            {/* QUICK TRUST BADGES */}
            <div style={{ display: 'flex', gap: '20px', marginTop: '35px', color: 'var(--text-secondary)', fontSize: '0.85rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                <CheckCircle size={16} color="var(--accent-neon)" /> No SIM Required
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                <CheckCircle size={16} color="var(--accent-neon)" /> E2E Encrypted (AES/GCM)
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                <CheckCircle size={16} color="var(--accent-neon)" /> 100% Free & Open
              </div>
            </div>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, x: 50 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 1, delay: 0.3 }}
            style={{ flex: '0 0 auto' }}
          >
            <OfflineSimulator />
          </motion.div>
          
        </div>
        
        <motion.div 
          animate={{ y: [0, 10, 0] }} 
          transition={{ repeat: Infinity, duration: 2 }}
          style={{ position: 'absolute', bottom: '25px', color: 'var(--accent-purple)' }}
        >
          <a href="#use-cases" style={{ color: 'inherit' }}><ArrowDownCircle size={30} /></a>
        </motion.div>
      </section>

      {/* SECTION 2: TARGET CUSTOMERS / VALUE PROPOSITION */}
      <section id="use-cases" style={{ padding: '80px 20px', maxWidth: '1200px', margin: '0 auto' }}>
        <div style={{ textAlign: 'center', marginBottom: '60px' }}>
          <h2 style={{ fontSize: '2.5rem', fontWeight: '800', marginBottom: '15px' }}>
            Built for Places Where Cellular Networks Fail
          </h2>
          <p style={{ color: 'var(--text-secondary)', fontSize: '1.15rem', maxWidth: '700px', margin: '0 auto' }}>
            Centralized telecommunications leave massive deadzones. Deadzone Messenger steps in wherever cell towers and Wi-Fi networks go dark.
          </p>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: '25px' }}>
          
          <motion.div 
            whileHover={{ y: -5 }} 
            className="glass-panel" 
            style={{ padding: '30px', borderTop: '3px solid var(--accent-neon)' }}
          >
            <div style={{ width: '48px', height: '48px', borderRadius: '12px', background: 'rgba(0, 240, 255, 0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: '20px' }}>
              <MapPin size={26} color="var(--accent-neon)" />
            </div>
            <h3 style={{ fontSize: '1.3rem', marginBottom: '12px' }}>Hikers & Remote Treks</h3>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem', lineHeight: '1.6' }}>
              Coordinate with friends across trails, remote valleys, and campsites miles beyond the nearest cell tower without expensive satellite hardware.
            </p>
          </motion.div>

          <motion.div 
            whileHover={{ y: -5 }} 
            className="glass-panel" 
            style={{ padding: '30px', borderTop: '3px solid var(--accent-purple)' }}
          >
            <div style={{ width: '48px', height: '48px', borderRadius: '12px', background: 'rgba(176, 92, 255, 0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: '20px' }}>
              <Users size={26} color="var(--accent-purple)" />
            </div>
            <h3 style={{ fontSize: '1.3rem', marginBottom: '12px' }}>Campus & Concrete Deadzones</h3>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem', lineHeight: '1.6' }}>
              Thick concrete, high-rise lecture halls, and basement labs block cell signals. Deadzone meshes phones together through walls effortlessly.
            </p>
          </motion.div>

          <motion.div 
            whileHover={{ y: -5 }} 
            className="glass-panel" 
            style={{ padding: '30px', borderTop: '3px solid var(--accent-neon)' }}
          >
            <div style={{ width: '48px', height: '48px', borderRadius: '12px', background: 'rgba(0, 240, 255, 0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: '20px' }}>
              <Smartphone size={26} color="var(--accent-neon)" />
            </div>
            <h3 style={{ fontSize: '1.3rem', marginBottom: '12px' }}>Stadiums & Crowded Events</h3>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem', lineHeight: '1.6' }}>
              When 50,000+ people overload local cell sites, cellular data drops to zero. Mesh connections bypass towers entirely by routing locally.
            </p>
          </motion.div>

          <motion.div 
            whileHover={{ y: -5 }} 
            className="glass-panel" 
            style={{ padding: '30px', borderTop: '3px solid var(--accent-purple)' }}
          >
            <div style={{ width: '48px', height: '48px', borderRadius: '12px', background: 'rgba(176, 92, 255, 0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: '20px' }}>
              <Shield size={26} color="var(--accent-purple)" />
            </div>
            <h3 style={{ fontSize: '1.3rem', marginBottom: '12px' }}>Total Privacy & Zero Logs</h3>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem', lineHeight: '1.6' }}>
              No centralized servers, no telecom tracking, and no metadata collection. Your communications stay strictly between you and your peers.
            </p>
          </motion.div>

        </div>
      </section>

      {/* SECTION 3: HOW IT WORKS (Mesh Routing Visual) */}
      <section id="how-it-works" style={{ padding: '80px 20px', maxWidth: '1100px', margin: '0 auto' }}>
        <div style={{ textAlign: 'center', marginBottom: '50px' }}>
          <h2 style={{ fontSize: '2.5rem', fontWeight: '800', marginBottom: '15px' }}>
            How Decentralized Mesh Routing Works
          </h2>
          <p style={{ color: 'var(--text-secondary)', fontSize: '1.15rem', maxWidth: '750px', margin: '0 auto' }}>
            Instead of routing messages through distant cell towers, Deadzone Messenger connects directly device-to-device.
            Intermediate phones act as encrypted relays, hopping your packet to its destination.
          </p>
        </div>

        <motion.div 
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="glass-panel" 
          style={{ padding: '35px' }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '20px' }}>
            <Radio size={28} color="var(--accent-neon)" />
            <div>
              <h3 style={{ fontSize: '1.4rem' }}>Interactive Multi-Hop Simulation</h3>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem' }}>Watch data packets route dynamically around offline nodes in real time.</p>
            </div>
          </div>
          <MeshSimulator />
        </motion.div>

        {/* 3 CORE PILLARS */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '25px', marginTop: '30px' }}>
          <div className="glass-panel" style={{ padding: '25px' }}>
            <Shield size={28} color="var(--accent-purple)" style={{ marginBottom: '15px' }} />
            <h3 style={{ fontSize: '1.2rem', marginBottom: '10px' }}>Military-Grade Security</h3>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem', lineHeight: '1.6' }}>
              Every single byte is encrypted with AES/GCM. Intermediate relay devices cannot inspect or alter message contents.
            </p>
          </div>

          <div className="glass-panel" style={{ padding: '25px' }}>
            <ImageIcon size={28} color="var(--accent-neon)" style={{ marginBottom: '15px' }} />
            <h3 style={{ fontSize: '1.2rem', marginBottom: '10px' }}>Rich Offline Media</h3>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem', lineHeight: '1.6' }}>
              Send crystal-clear voice notes and full-resolution images seamlessly over peer-to-peer Wi-Fi Direct.
            </p>
          </div>

          <div className="glass-panel" style={{ padding: '25px' }}>
            <Bookmark size={28} color="var(--accent-purple)" style={{ marginBottom: '15px' }} />
            <h3 style={{ fontSize: '1.2rem', marginBottom: '10px' }}>Complete Control</h3>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem', lineHeight: '1.6' }}>
              Permanent offline message bookmarking, along with instantaneous 'Delete for Me' and 'Delete for Everyone' options.
            </p>
          </div>
        </div>
      </section>

      {/* SECTION 4: INTERACTIVE DEMO (Dual-Device Simulator) */}
      <section id="demo" style={{ padding: '80px 20px', maxWidth: '1100px', margin: '0 auto' }}>
        <div style={{ textAlign: 'center', marginBottom: '40px' }}>
          <div style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '8px',
            padding: '5px 14px',
            borderRadius: '20px',
            background: 'rgba(176, 92, 255, 0.1)',
            border: '1px solid rgba(176, 92, 255, 0.3)',
            color: 'var(--accent-purple)',
            fontSize: '0.85rem',
            fontWeight: '600',
            marginBottom: '15px'
          }}>
            INTERACTIVE PRODUCT TOUR
          </div>
          <h2 style={{ fontSize: '2.5rem', fontWeight: '800', marginBottom: '15px' }}>
            Live Device-to-Device Experience
          </h2>
          <p style={{ color: 'var(--text-secondary)', fontSize: '1.15rem', maxWidth: '700px', margin: '0 auto' }}>
            Interact with the synchronized phone simulators below to see offline pairing, instant messaging, media exchange, and remote message deletion in action.
          </p>
        </div>

        <motion.div 
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="glass-panel" 
          style={{ padding: '35px' }}
        >
          <DualDeviceSimulator />
        </motion.div>
      </section>

      {/* SECTION 5: UNDER THE HOOD (Tech Stack) */}
      <section id="tech" style={{ padding: '80px 20px', maxWidth: '1200px', margin: '0 auto' }}>
        <div style={{ textAlign: 'center', marginBottom: '50px' }}>
          <h2 style={{ fontSize: '2.5rem', fontWeight: '800', marginBottom: '15px', color: 'var(--accent-neon)' }}>
            Engineered with Modern Native Android
          </h2>
          <p style={{ color: 'var(--text-secondary)', fontSize: '1.15rem', maxWidth: '700px', margin: '0 auto' }}>
            Built 100% natively from scratch with zero third-party cloud SDKs or tracking services.
          </p>
        </div>
        
        <div style={{ display: 'grid', gap: '25px', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))' }}>
          <div className="glass-panel" style={{ padding: '30px' }}>
            <Code size={30} color="var(--accent-purple)" style={{ marginBottom: '15px' }} />
            <h3 style={{ fontSize: '1.25rem', marginBottom: '10px' }}>Kotlin & Jetpack Compose</h3>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem', lineHeight: '1.6' }}>
              Blazing-fast, declarative UI with fluid animations, zero XML, and modern Android architecture best practices.
            </p>
          </div>

          <div className="glass-panel" style={{ padding: '30px' }}>
            <Network size={30} color="var(--accent-neon)" style={{ marginBottom: '15px' }} />
            <h3 style={{ fontSize: '1.25rem', marginBottom: '10px' }}>Google Nearby Connections</h3>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem', lineHeight: '1.6' }}>
              Low-latency P2P mesh discovery that intelligently hops between Bluetooth Low Energy (BLE) and high-speed Wi-Fi Direct.
            </p>
          </div>

          <div className="glass-panel" style={{ padding: '30px' }}>
            <Database size={30} color="var(--accent-purple)" style={{ marginBottom: '15px' }} />
            <h3 style={{ fontSize: '1.25rem', marginBottom: '10px' }}>Room DB & Scoped Storage</h3>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem', lineHeight: '1.6' }}>
              All chats and media stay strictly sandboxed locally on your hardware with instantaneous queries and zero cloud leakage.
            </p>
          </div>

          <div className="glass-panel" style={{ padding: '30px' }}>
            <Lock size={30} color="var(--accent-neon)" style={{ marginBottom: '15px' }} />
            <h3 style={{ fontSize: '1.25rem', marginBottom: '10px' }}>AES/GCM Encryption</h3>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem', lineHeight: '1.6' }}>
              Cryptographically verified End-to-End Encryption prevents any eavesdropping from intermediate relay nodes.
            </p>
          </div>
        </div>
      </section>

      {/* SECTION 6: CALL TO ACTION & CONTACT (Rubric Compliance) */}
      <section id="contact" style={{ padding: '100px 20px', maxWidth: '1000px', margin: '0 auto' }}>
        <motion.div 
          initial={{ opacity: 0, scale: 0.95 }}
          whileInView={{ opacity: 1, scale: 1 }}
          viewport={{ once: true }}
          className="glass-panel"
          style={{
            padding: '50px 40px',
            textAlign: 'center',
            background: 'linear-gradient(180deg, rgba(28, 29, 37, 0.95), rgba(15, 16, 21, 0.98))',
            border: '1px solid rgba(0, 240, 255, 0.25)',
            boxShadow: '0 0 40px rgba(0, 240, 255, 0.15)'
          }}
        >
          <div style={{
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
            width: '60px',
            height: '60px',
            borderRadius: '50%',
            background: 'rgba(0, 240, 255, 0.1)',
            marginBottom: '20px'
          }}>
            <Radio size={30} color="var(--accent-neon)" />
          </div>

          <h2 style={{ fontSize: '2.8rem', fontWeight: '800', marginBottom: '15px' }}>
            Ready for Off-Grid Freedom?
          </h2>
          <p style={{ color: 'var(--text-secondary)', fontSize: '1.2rem', maxWidth: '650px', margin: '0 auto 35px' }}>
            Join the early beta testing group, request the latest Android APK build, or connect with the creator for project collaborations.
          </p>

          {/* EARLY ACCESS FORM */}
          <form onSubmit={handleSubmit} style={{ display: 'flex', flexWrap: 'wrap', gap: '12px', justifyContent: 'center', maxWidth: '520px', margin: '0 auto 40px' }}>
            <input 
              type="email" 
              placeholder="Enter your email for early APK access..."
              value={emailInput}
              onChange={(e) => setEmailInput(e.target.value)}
              required
              style={{
                flex: '1 1 280px',
                padding: '14px 20px',
                borderRadius: '10px',
                background: 'rgba(255, 255, 255, 0.05)',
                border: '1px solid rgba(255, 255, 255, 0.15)',
                color: '#fff',
                fontSize: '1rem',
                outline: 'none'
              }}
            />
            <button 
              type="submit"
              style={{
                background: 'linear-gradient(135deg, var(--accent-neon), var(--accent-purple))',
                color: '#0f1015',
                padding: '14px 24px',
                borderRadius: '10px',
                border: 'none',
                fontWeight: '700',
                fontSize: '1rem',
                cursor: 'pointer',
                display: 'inline-flex',
                alignItems: 'center',
                gap: '8px'
              }}
            >
              <Send size={16} /> Request APK
            </button>
          </form>

          {submitted && (
            <div style={{ color: 'var(--accent-neon)', marginBottom: '30px', fontWeight: '600' }}>
              ✓ Thank you! Opening your email client to connect with Subhash Chowdary Chandra.
            </div>
          )}

          {/* CONTACT & FOUNDER CARD */}
          <div style={{
            background: 'rgba(255, 255, 255, 0.03)',
            borderRadius: '16px',
            padding: '30px',
            maxWidth: '650px',
            margin: '0 auto',
            border: '1px solid rgba(255, 255, 255, 0.07)',
            textAlign: 'left'
          }}>
            <h3 style={{ fontSize: '1.2rem', marginBottom: '8px', color: 'var(--text-primary)' }}>
              Project Creator & Developer
            </h3>
            <p style={{ fontSize: '1.1rem', fontWeight: '700', color: 'var(--accent-neon)', marginBottom: '15px' }}>
              Subhash Chowdary Chandra
            </p>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem', lineHeight: '1.6', marginBottom: '20px' }}>
              Android Architect & Decentralized Systems Developer. Open for technical inquiries, research collaborations, and beta feedback.
            </p>

            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '15px' }}>
              <a 
                href="mailto:subhashchowdarychandra@gmail.com"
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '8px',
                  background: 'rgba(0, 240, 255, 0.1)',
                  color: 'var(--accent-neon)',
                  padding: '10px 18px',
                  borderRadius: '8px',
                  textDecoration: 'none',
                  fontSize: '0.9rem',
                  fontWeight: '600',
                  border: '1px solid rgba(0, 240, 255, 0.25)'
                }}
              >
                <Mail size={16} /> Email Founder
              </a>

              <a 
                href="https://www.linkedin.com/in/subhash-chowdary-chandra" 
                target="_blank" 
                rel="noreferrer"
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '8px',
                  background: 'rgba(176, 92, 255, 0.1)',
                  color: 'var(--accent-purple)',
                  padding: '10px 18px',
                  borderRadius: '8px',
                  textDecoration: 'none',
                  fontSize: '0.9rem',
                  fontWeight: '600',
                  border: '1px solid rgba(176, 92, 255, 0.25)'
                }}
              >
                <ExternalLink size={16} /> LinkedIn Profile
              </a>

              <a 
                href="https://github.com/Subhash-Chowdary-Chandra/Deadzone-Messenger" 
                target="_blank" 
                rel="noreferrer"
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '8px',
                  background: 'rgba(255, 255, 255, 0.08)',
                  color: 'var(--text-primary)',
                  padding: '10px 18px',
                  borderRadius: '8px',
                  textDecoration: 'none',
                  fontSize: '0.9rem',
                  fontWeight: '600',
                  border: '1px solid rgba(255, 255, 255, 0.15)'
                }}
              >
                <ExternalLink size={16} /> GitHub Repository
              </a>
            </div>
          </div>
        </motion.div>
      </section>

      {/* FOOTER */}
      <footer style={{ padding: '40px 20px', borderTop: '1px solid rgba(255,255,255,0.06)', textAlign: 'center', background: '#0a0a0d' }}>
        <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem' }}>
          &copy; {new Date().getFullYear()} <strong>Deadzone Messenger</strong>. All rights reserved.
        </p>
        <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginTop: '8px' }}>
          Conceived, designed, and developed by <span style={{ color: 'var(--accent-neon)', fontWeight: 'bold' }}>Subhash Chowdary Chandra</span>
        </p>
      </footer>

    </div>
  );
}

export default App;