import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuthStore } from './store.js'
import Login from './pages/Login.jsx'
import StudentLayout from './layouts/StudentLayout.jsx'
import StudentHome from './pages/student/Home.jsx'
import StudentBasicEvaluation from './pages/student/BasicEvaluation.jsx'
import StudentAbilityEvaluation from './pages/student/AbilityEvaluation.jsx'
import StudentScholarships from './pages/student/Scholarships.jsx'
import StudentApplications from './pages/student/Applications.jsx'
import CounselorLayout from './layouts/CounselorLayout.jsx'
import CounselorStudents from './pages/counselor/Students.jsx'
import CounselorReview from './pages/counselor/Review.jsx'
import CounselorApplications from './pages/counselor/Applications.jsx'
import AdminLayout from './layouts/AdminLayout.jsx'
import AdminDashboard from './pages/admin/Dashboard.jsx'
import AdminYears from './pages/admin/Years.jsx'
import AdminProjects from './pages/admin/Projects.jsx'
import AdminRanking from './pages/admin/Ranking.jsx'
import AdminImport from './pages/admin/Import.jsx'
import ResultsPublic from './pages/ResultsPublic.jsx'
import ChangePassword from './pages/ChangePassword.jsx'

function Protected({ role, children }) {
  const { token, user } = useAuthStore()
  if (!token) return <Navigate to="/login" replace />
  if (role && user?.role !== role) return <Navigate to="/login" replace />
  return children
}

function RootRedirect() {
  const { token, user } = useAuthStore()
  if (!token) return <Navigate to="/login" replace />
  if (user?.role === 'STUDENT') return <Navigate to="/student" replace />
  if (user?.role === 'COUNSELOR') return <Navigate to="/counselor" replace />
  if (user?.role === 'ADMIN') return <Navigate to="/admin" replace />
  return <Navigate to="/login" replace />
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/results" element={<ResultsPublic />} />

      <Route path="/student" element={<Protected role="STUDENT"><StudentLayout /></Protected>}>
        <Route index element={<StudentHome />} />
        <Route path="basic-eval" element={<StudentBasicEvaluation />} />
        <Route path="ability-eval" element={<StudentAbilityEvaluation />} />
        <Route path="scholarships" element={<StudentScholarships />} />
        <Route path="applications" element={<StudentApplications />} />
        <Route path="password" element={<ChangePassword />} />
      </Route>

      <Route path="/counselor" element={<Protected role="COUNSELOR"><CounselorLayout /></Protected>}>
        <Route index element={<CounselorStudents />} />
        <Route path="review" element={<CounselorReview />} />
        <Route path="applications" element={<CounselorApplications />} />
        <Route path="password" element={<ChangePassword />} />
      </Route>

      <Route path="/admin" element={<Protected role="ADMIN"><AdminLayout /></Protected>}>
        <Route index element={<AdminDashboard />} />
        <Route path="years" element={<AdminYears />} />
        <Route path="projects" element={<AdminProjects />} />
        <Route path="ranking" element={<AdminRanking />} />
        <Route path="import" element={<AdminImport />} />
        <Route path="password" element={<ChangePassword />} />
      </Route>

      <Route path="/" element={<RootRedirect />} />
      <Route path="*" element={<RootRedirect />} />
    </Routes>
  )
}
